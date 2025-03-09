import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.net.URLEncoder


suspend fun respToJsonArr(response: HttpResponse): JsonArray {
    return try {
        val jsonString = response.body<String>()
        Json.parseToJsonElement(jsonString).jsonArray
    } catch (e: Exception) {
        JsonArray(emptyList())
    }
}

fun setupClient(): HttpClient {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
    return client
}

fun parseCountry(jsonElement: JsonElement): Pair<String, String>? {
    val json = jsonElement.jsonObject

    val countryMaybe = json["country"]?.jsonPrimitive?.contentOrNull

    val city = json["city"]?.jsonPrimitive?.contentOrNull
    val street = json["street_name"]?.jsonPrimitive?.contentOrNull
    val building = json["building_number"]?.jsonPrimitive?.contentOrNull ?: ""
    val address = if (city == null || street == null) {
        "Unknown address"
    } else {
        "$building $street, $city"
    }

    return countryMaybe?.let { country ->
        Pair(country, "$address...")
    }
}

suspend fun getAddresses(num: Int): MutableList<Pair<String, String>> {
    val client = setupClient()
    val response: HttpResponse = client.get("https://random-data-api.com/api/v2/addresses?size=$num")

    val addressArr = mutableListOf<Pair<String, String>>()

    if (num == 1) {
        val jsonString = response.body<String>()
        val jsonObject = Json.parseToJsonElement(jsonString)

        parseCountry(jsonObject)?.let { addPair ->
            addressArr.add(addPair)
        }
    }
    else {
        for (jsonElement in respToJsonArr(response)) {
            val addrMaybe = parseCountry(jsonElement)

            addrMaybe?.let { addrPair ->
                addressArr.add(addrPair)
            }
        }
    }

    client.close()
    return addressArr
}

suspend fun getCountryInfo(country: String): String {
    val client = setupClient()
    var encodedCountry = withContext(Dispatchers.IO) {
        URLEncoder.encode(country, "UTF-8")
    }
    encodedCountry = encodedCountry.replace("+", "%20")

    val url = "https://restcountries.com/v3.1/name/$encodedCountry"
    println("GET $url")
    val response: HttpResponse = client.get(url)
    val arr = respToJsonArr(response)
    if (arr.size <= 0) {
        return "Population Unknown"
    }
    val json = arr[0].jsonObject
    val population = json["population"]?.jsonPrimitive?.contentOrNull ?: "Unknown"

    return "Population: $population"
}

@Composable
@Preview
fun App() {
    var sliderValue by remember { mutableStateOf(1f) }
    val scope = rememberCoroutineScope()
    val maxValue = 5
    var addresses by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = 1f..maxValue.toFloat(), // Adjust the range
                steps = maxValue - 2,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Number of Addresses: ${sliderValue.toInt()}", modifier = Modifier.padding(top = 8.dp))

            Button(onClick = {
                scope.launch {
                    addresses = getAddresses(sliderValue.toInt())

                    for (i in addresses.indices) {
                        val country = addresses[i].first
                        val fullAddress = addresses[i].second

                        launch {
                            val countryInfo = getCountryInfo(country)

                            addresses = addresses.mapIndexed { index, pair ->
                                if (index == i) {
                                    Pair(country, fullAddress + countryInfo)
                                } else {
                                    pair // Keep the rest the same
                                }
                            }
                        }
                    }
                }
            }) {
                Text("Get Address")
            }

            if (addresses.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    addresses.forEachIndexed { index, (country, address) ->
                        Text(
                            text = "${index + 1}: $country, $address",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}


fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
fun main2() {
    runBlocking {
        val arr = getAddresses(1);
        for ((country, address) in arr) {
            println("$country: $address")
        }
    }
}
