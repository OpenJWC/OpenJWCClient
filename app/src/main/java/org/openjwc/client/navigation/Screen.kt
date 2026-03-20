import kotlinx.serialization.Serializable
import org.openjwc.client.net.models.FetchedNotice

sealed interface Screen {
    @Serializable object Settings: Screen
    @Serializable object Main : Screen
    @Serializable object UploadNews : Screen
    @Serializable object About : Screen
    @Serializable object Host : Screen
    @Serializable object Auth : Screen
    @Serializable object Theme : Screen
    @Serializable object Review : Screen
    @Serializable
    data class NewsDetail(val fetchedNotice: FetchedNotice) : Screen
}