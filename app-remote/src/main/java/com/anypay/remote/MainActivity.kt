private fun uploadMediaFile(uri: Uri) {
        val host = activeDevice?.host ?: return
        val contentResolver = applicationContext.contentResolver
        val mimeType = contentResolver.getType(uri) ?: ""
        val isVideo = mimeType.startsWith("video")
        val type = if (isVideo) "video" else "image"

        var fileName = "media_${System.currentTimeMillis()}"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                fileName = cursor.getString(nameIndex)
            }
        }

        var fileDurationSec = 0
        if (isVideo) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(this, uri)
                val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                fileDurationSec = ((time?.toLong() ?: 0L) / 1000).toInt()
            } catch (_: Exception) {}
        }

        Toast.makeText(this, "Yükleniyor, lütfen bekleyin...", Toast.LENGTH_SHORT).show()

        // RAM Çökmesini Önleyen Akış (Streaming) Yükleyici:
        val customRequestBody = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaTypeOrNull()

            override fun contentLength(): Long {
                return try {
                    contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                } catch (_: Exception) { -1L }
            }

            override fun writeTo(sink: okio.BufferedSink) {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        sink.write(buffer, 0, read)
                    }
                }
            }
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, customRequestBody)
            .build()

        val uploadRequest = Request.Builder()
            .url("http://$host/api/upload?filename=$fileName")
            .post(requestBody)
            .build()

        httpClient.newCall(uploadRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Yükleme Başarısız!", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val dur = edtDuration.text.toString().toIntOrNull() ?: 0
                    val wait = edtWaitAfter.text.toString().toIntOrNull() ?: 0
                    val anim = animValues[spinnerAnim.selectedItemPosition]

                    val newItem = MediaItemModel(
                        id = System.currentTimeMillis().toString(),
                        fileName = fileName,
                        type = type,
                        durationSec = dur,
                        waitAfterSec = wait,
                        animation = anim,
                        fileDurationSec = fileDurationSec
                    )
                    playlist.add(newItem)
                    syncPlaylistToMenuboard()
                }
            }
        })
    }
