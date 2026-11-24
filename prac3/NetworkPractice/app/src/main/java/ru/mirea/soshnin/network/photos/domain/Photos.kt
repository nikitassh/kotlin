package ru.mirea.soshnin.network.photos.domain

data class Photo(
    val id: String,
    val author: String,
    val width: Int,
    val height: Int,
    val pageUrl: String,
    val downloadUrl: String
) {
    // маленькая превью через параметры picsum
    val thumbnailUrl get() = "https://picsum.photos/id/$id/300/200"
}

interface PhotoRepository {
    suspend fun getPhotos(page: Int): List<Photo>
    suspend fun getPhoto(id: String): Photo
    suspend fun download(photo: Photo): ByteArray
}

class GetPhotosUseCase(private val repo: PhotoRepository) {
    suspend operator fun invoke(page: Int = 1) = repo.getPhotos(page)
}

class GetPhotoUseCase(private val repo: PhotoRepository) {
    suspend operator fun invoke(id: String) = repo.getPhoto(id)
}

class DownloadPhotoUseCase(private val repo: PhotoRepository) {
    suspend operator fun invoke(photo: Photo) = repo.download(photo)
}
