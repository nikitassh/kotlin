import kotlinx.serialization.Serializable

@Serializable
data class User(val id: Int, val name: String)

@Serializable
data class SaleItem(val product: String, val qty: Int, val revenue: Int)

@Serializable
data class Sales(val today: String, val items: List<SaleItem>)

@Serializable
data class Weather(val city: String, val temp: Int, val condition: String)
