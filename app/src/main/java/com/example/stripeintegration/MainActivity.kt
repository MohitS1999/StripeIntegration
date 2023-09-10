package com.example.stripeintegration

import android.R
import android.app.ProgressDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Telephony.Mms.Addr
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.stripeintegration.databinding.ActivityMainBinding
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import okhttp3.HttpUrl.Companion.get
import okhttp3.HttpUrl.Companion.toHttpUrl

private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {
    private val BACKEND_URL =
        "http://10.0.2.2:4242/payment-sheet" //4242 is port mentioned in server i.e index.js


    private lateinit var binding: ActivityMainBinding
    lateinit var paymentSheet: PaymentSheet
    lateinit var customerConfig: PaymentSheet.CustomerConfiguration
    lateinit var paymentIntentClientSecret: String
    private var amount: Int = 0



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        



        binding.payButton.setOnClickListener {
            if (TextUtils.isEmpty(binding.amountId.text.toString())){
                Toast.makeText(this,"Amount cannot be empty",Toast.LENGTH_SHORT).show()
            } else{
                Log.d(TAG, "onCreate: clicked on pay button")
                runOnUiThread{ getDetails()}
                
            }
        }
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        val googlePayConfiguration = PaymentSheet.GooglePayConfiguration(
            environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
            countryCode = "US",
            currencyCode = "USD" // Required for Setup Intents, optional for Payment Intents
        )

    }

    private fun getDetails() {
        BACKEND_URL.httpPost().responseJson { _, _, result ->
            try {
                Log.d(TAG, "getDetails: try top")
                if (result is Result.Success) {
                    try {
                        val responseJson = result.get().obj()
                        Log.d(TAG, "getDetails: ${responseJson.getString("customer")}")
                        paymentIntentClientSecret = responseJson.getString("paymentIntent")
                        customerConfig = PaymentSheet.CustomerConfiguration(
                            responseJson.getString("customer"),
                            responseJson.getString("ephemeralKey")
                        )
                        val publishableKey = responseJson.getString("publishableKey")
                        Log.d(TAG, "getDetails: publishkey $publishableKey")
                        PaymentConfiguration.init(this, publishableKey)
                        runOnUiThread { presentPaymentSheet() }
                    }catch (e:Exception){
                        Log.d(TAG, "getDetails: catch exception")
                        Log.d(TAG, "onCreate: ${e.message}")
                    }

                }
                if (result is Result.Failure){
                    Log.d(TAG, "getDetails: ${result.toString()}")
                }
            }catch (e:Exception){
                Log.d(TAG, "getDetails: top catch ${e.message}")
            }
            
        }
    }

    private fun presentPaymentSheet() {
        Log.d(TAG, "presentPaymentSheet: ")
        val googlePayConfiguration = PaymentSheet.GooglePayConfiguration(
            environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
            countryCode = "US",
            currencyCode = "USD" // Required for Setup Intents, optional for Payment Intents
        )
        paymentSheet.presentWithPaymentIntent(
            paymentIntentClientSecret,
            PaymentSheet.Configuration(
                merchantDisplayName = "My merchant name",
                customer = customerConfig,
                // Set `allowsDelayedPaymentMethods` to true if your business handles
                // delayed notification payment methods like US bank accounts.
                allowsDelayedPaymentMethods = true,
                googlePay = googlePayConfiguration
            )
        )




    }
    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when(paymentSheetResult) {
            is PaymentSheetResult.Canceled -> {
                Log.d(TAG, "onPaymentSheetResult: CANCELLED")
            }
            is PaymentSheetResult.Failed -> {
                print("Error: ${paymentSheetResult.error}")
                Log.d(TAG, "onPaymentSheetResult: FAILED")
            }
            is PaymentSheetResult.Completed -> {
                // Display for example, an order confirmation screen
                Log.d(TAG, "onPaymentSheetResult: COMPLETED")
                print("Completed")
            }
        }
    }


}