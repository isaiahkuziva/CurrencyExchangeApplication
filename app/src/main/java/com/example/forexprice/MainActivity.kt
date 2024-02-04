package com.example.forexprice

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.content.Intent
import android.os.Handler
import android.transition.Visibility
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import okhttp3.OkHttpClient
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.math.RoundingMode

object RetrofitClient {
    private const val BASE_URL = "https://api-fxpractice.oanda.com/"

    val apiService: OandaApiService by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OandaApiService::class.java)
    }
}

interface OandaApiService {
    @Headers("Authorization: Bearer bd81e538f5e0381bde8d3dddd17eefdf-2953e58e630fa2f76c6c632dcdfbaa42")
    @GET("v3/accounts/101-011-28219270-001/pricing")
    fun getExchangeRates(
        @Query("instruments") instruments: String
    ): Call<OandaApiResponse>
}


data class OandaApiResponse(
    val time: String,
    val prices: List<Price>
)

data class Price(
    val type: String,
    val time: String,
    val bids: List<BidAsk>,
    val asks: List<BidAsk>,
    @SerializedName("closeoutBid")
    val closeoutBid: String,
    @SerializedName("closeoutAsk")
    val closeoutAsk: String,
    val status: String,
    val tradeable: Boolean,
    @SerializedName("quoteHomeConversionFactors")
    val conversionFactors: ConversionFactors,
    val instrument: String
)

data class BidAsk(
    val price: String,
    val liquidity: Long
)

data class ConversionFactors(
    @SerializedName("positiveUnits")
    val positiveUnits: String,
    @SerializedName("negativeUnits")
    val negativeUnits: String
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onConvertButtonClick(view: View) {
        // Retrieve the USD amount from the EditText
        val editTextAmount: EditText = findViewById(R.id.editTextAmount)
        val usdAmount = editTextAmount.text.toString().toDoubleOrNull() ?: 0.0

        val detailsScreen:LinearLayout = findViewById(R.id.details_screen);
        val loadingScreen:LinearLayout = findViewById(R.id.loading_screen);
        detailsScreen.visibility=View.GONE;
        loadingScreen.visibility=View.VISIBLE;

        val targetCurrencies = listOf("EUR_USD", "USD_JPY", "USD_CAD", "USD_CHF", "GBP_USD", "AUD_USD", "NZD_USD", "USD_SGD", "USD_ZAR", "USD_MXN")

        val resultTextView: TextView = findViewById(R.id.textViewCurrency1)
        val resultScreen:LinearLayout = findViewById(R.id.result_screen);

        val handler = Handler()
        targetCurrencies.forEach { instrument ->
            RetrofitClient.apiService.getExchangeRates(instrument).enqueue(object : Callback<OandaApiResponse> {
                override fun onResponse(call: Call<OandaApiResponse>, response: Response<OandaApiResponse>) {
                    loadingScreen.visibility=View.GONE;
                    resultScreen.visibility=View.VISIBLE;
                    var I:String = instrument.replace("_","")
                    I = I.replace("USD","")
                    if (response.isSuccessful) {
                        val price = response.body()?.prices?.firstOrNull()
                        val convertedAmount = price?.asks?.firstOrNull()?.price?.toDoubleOrNull()?.times(usdAmount) ?: 0.0
                        val roundedConvertedAmount = BigDecimal(convertedAmount).setScale(2, RoundingMode.HALF_UP).toDouble()

                        handler.post {
                            resultTextView.append("$instrument: $usdAmount USD is equal to $roundedConvertedAmount $I\n")
                        }
                    } else {
                        resultTextView.append("$instrument Failed to retrieve exchange rates for $I\n")
                    }
                }

                override fun onFailure(call: Call<OandaApiResponse>, t: Throwable) {
                    resultTextView.append("Network error: ${t.message}\n")
                }
            })
        }
    }

    fun onBackButtonClick(view: View){
        val detailsScreen:LinearLayout = findViewById(R.id.details_screen);
        val loadingScreen:LinearLayout = findViewById(R.id.loading_screen);
        val resultScreen:LinearLayout = findViewById(R.id.result_screen);
        val resultTextView: TextView = findViewById(R.id.textViewCurrency1)
        resultTextView.text = ""
        detailsScreen.visibility=View.VISIBLE;
        loadingScreen.visibility=View.GONE;
        resultScreen.visibility=View.GONE;
    }
}
