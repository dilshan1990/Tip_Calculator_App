package com.example.tipcalculator

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.w3c.dom.Text
import kotlin.math.ceil

class MainActivity : AppCompatActivity() {

    private lateinit var billAmountEditText: EditText
    private lateinit var tipPercentageRadioGroup: RadioGroup
    private lateinit var customTipEditText: EditText
    private lateinit var currencySpinner: Spinner
    private lateinit var selectCurrencyTextView: TextView
    private lateinit var calculateButton: Button
    private lateinit var tipAmountTextView: TextView
    private lateinit var totalBillTextView: TextView
    private lateinit var roundTipSwitch: Switch
    private lateinit var splitBillSwitch: Switch
    private lateinit var numberOfPeopleEditText: EditText
    private lateinit var selectedCurrency: String


    private val themeTitleList = arrayOf("Light","Dark","Auto (System Default)")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val changeThemeBtn = findViewById<Button>(R.id.ChangeTheme)
       // val themeTxt = findViewById<TextView>(R.id.themeTxt)


        val sharedPrefernceManger = SharedPrefernceManger(this)
        var checkedTheme = sharedPrefernceManger.theme
       // themeTxt.text = "Theme: ${themeTitleList[sharedPrefernceManger.theme]}"
        val themeDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Theme")
            .setPositiveButton("Ok") { _, _ ->
            sharedPrefernceManger.theme = checkedTheme
                AppCompatDelegate.setDefaultNightMode(sharedPrefernceManger.themeFlag[checkedTheme])
           //     themeTxt.text = "Theme: ${themeTitleList[sharedPrefernceManger.theme]}"

            }
            .setSingleChoiceItems(themeTitleList,checkedTheme){_, which ->
                checkedTheme = which
            }
            .setCancelable(false)

        changeThemeBtn.setOnClickListener{
            themeDialog.show()

        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Tip Calculator"

        roundTipSwitch = findViewById(R.id.roundTipSwitch)
        splitBillSwitch = findViewById(R.id.splitBillSwitch)
        numberOfPeopleEditText = findViewById(R.id.numberOfPeopleEditText)

        // New code to toggle visibility of numberOfPeopleEditText
        splitBillSwitch.setOnCheckedChangeListener { _, isChecked ->
            numberOfPeopleEditText.visibility = if (isChecked) View.VISIBLE else View.INVISIBLE
        }

        // Update visibility based on the initial state of splitBillSwitch
        updateNumberOfPeopleVisibility()

        tipPercentageRadioGroup = findViewById(R.id.tipPercentageRadioGroup)
        customTipEditText = findViewById(R.id.customTipEditText)

        billAmountEditText = findViewById(R.id.billAmountEditText)
        currencySpinner = findViewById(R.id.currencySpinner)
        selectCurrencyTextView = findViewById(R.id.selectCurrencyTextView)
        calculateButton = findViewById(R.id.calculateButton)
        tipAmountTextView = findViewById(R.id.tipAmountTextView)
        totalBillTextView = findViewById(R.id.totalBillTextView)

        val currencies = arrayOf("USD", "EUR", "GBP", "JPY", "CAD", "LKR")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        currencySpinner.adapter = adapter

        selectedCurrency = currencies[0]

        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                selectedCurrency = currencies[position]
                selectCurrencyTextView.text = "Selected Currency: $selectedCurrency"
                resetInputFields()
                calculateTip()
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // Handle when nothing is selected
            }
        }

        tipPercentageRadioGroup.setOnCheckedChangeListener { _, _ ->
            customTipEditText.visibility = if (R.id.radioCustom == tipPercentageRadioGroup.checkedRadioButtonId) View.VISIBLE else View.GONE
            calculateTip()
        }

        calculateButton.setOnClickListener {
            if (billAmountEditText.text.isNullOrBlank() || billAmountEditText.text.toString().toDoubleOrNull() == null) {
                showInvalidAmountPopup()
            } else {
                try {
                    val customTipPercentage = when {
                        tipPercentageRadioGroup.checkedRadioButtonId == R.id.radioCustom -> {
                            val customTipInput = customTipEditText.text.toString()
                            if (customTipInput.isNotBlank()) {
                                customTipInput.toDouble()
                            } else {
                                throw NumberFormatException()
                            }
                        }
                        else -> getSelectedTipPercentage()
                    }

                    // Log the customTipPercentage for debugging
                    Log.d("CustomTip", "Custom Tip Percentage: $customTipPercentage")

                    // Calculate tip, round off if needed, and update UI
                    calculateTip()

                    // Show the result popup
                    showResultPopup()

                } catch (e: NumberFormatException) {
                    Log.e("NumberFormatException", "Error converting customTipEditText to Double", e)
                    Toast.makeText(this, "Invalid custom tip percentage", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    // Log any other exception for debugging
                    Log.e("CalculateButton", "Error in calculateButton click", e)
                    Toast.makeText(this, "An error occurred", Toast.LENGTH_SHORT).show()
                }
            }
        }






        val exitButton: Button = findViewById(R.id.exitButton)
        exitButton.setOnClickListener {
            finish()
        }
    }

    private fun updateNumberOfPeopleVisibility() {
        numberOfPeopleEditText.visibility = if (splitBillSwitch.isChecked) View.VISIBLE else View.INVISIBLE
    }

    private fun getSelectedTipPercentage(): Double {
        val tipPercentageArray = arrayOf(5, 10, 15, 20)
        val checkedRadioButtonIndex = tipPercentageRadioGroup.indexOfChild(findViewById(tipPercentageRadioGroup.checkedRadioButtonId))
        return if (checkedRadioButtonIndex in tipPercentageArray.indices) {
            tipPercentageArray[checkedRadioButtonIndex].toDouble()
        } else {
            // Return a default value or handle the out-of-bounds case appropriately
            0.0
        }
    }
    private fun roundOffTip(tipAmount: Double): Double = ceil(tipAmount)

    private fun getCurrencySymbol(currencyCode: String): String {
        val currency = java.util.Currency.getInstance(currencyCode)
        return currency.symbol
    }

    private fun formatCurrency(amount: Double, currencyCode: String): String {
        val currencyFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.getDefault())
        currencyFormat.currency = java.util.Currency.getInstance(currencyCode)
        return currencyFormat.format(amount)
    }

    private fun calculateTip() {
        try {
            val billAmount = billAmountEditText.text.toString().toDoubleOrNull() ?: 0.0
            val tipPercentage = when {
                tipPercentageRadioGroup.checkedRadioButtonId == R.id.radioCustom -> {
                    customTipEditText.text.toString().toDoubleOrNull() ?: 0.0
                }
                else -> {
                    getSelectedTipPercentage()
                }
            }

            // Log the values for debugging
            Log.d("CalculateTip", "Bill Amount: $billAmount, Tip Percentage: $tipPercentage")

            var tipAmount = (billAmount * tipPercentage) / 100

            if (roundTipSwitch.isChecked) {
                tipAmount = roundOffTip(tipAmount)
            }

            var totalBill = billAmount + tipAmount
            if (splitBillSwitch.isChecked) {
                val numberOfPeople = numberOfPeopleEditText.text.toString().toIntOrNull() ?: 1
                if (numberOfPeople > 0) {
                    totalBill /= numberOfPeople
                }
            }

            val currencySymbol = getCurrencySymbol(selectedCurrency)
            val formattedTipAmount = formatCurrency(tipAmount, selectedCurrency)
            val formattedTotalBill = formatCurrency(totalBill, selectedCurrency)

            tipAmountTextView.text = getString(R.string.tip_amount_with_currency, formattedTipAmount, currencySymbol)
            totalBillTextView.text = getString(R.string.total_bill_with_currency_bold, formattedTotalBill, currencySymbol)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }











    private fun showResultPopup() {
        val billAmount = billAmountEditText.text.toString().toDoubleOrNull() ?: 0.0
        val tipPercentage = getSelectedTipPercentage()

        var tipAmount = (billAmount * tipPercentage) / 100

        if (roundTipSwitch.isChecked) {
            tipAmount = roundOffTip(tipAmount)
        }

        val totalBill = billAmount + tipAmount

        val currencySymbol = getCurrencySymbol(selectedCurrency)
        val formattedTipAmount = formatCurrency(tipAmount, selectedCurrency)
        val formattedTotalBill = formatCurrency(totalBill, selectedCurrency)

        val resultMessage = if (splitBillSwitch.isChecked) {
            val numberOfPeople = numberOfPeopleEditText.text.toString().toIntOrNull() ?: 1
            val paymentPerPerson = totalBill / numberOfPeople
            "Total Bill Amount: $formattedTotalBill\n" +
                    "Bill Amount: ${formatCurrency(billAmount, selectedCurrency)}\n" +
                    "Tip Amount: $formattedTipAmount\n" +
                    "Number of People Paying the Bill: $numberOfPeople\n" +
                    "Payment Per One Person: ${formatCurrency(paymentPerPerson, selectedCurrency)}"
        } else {
            "Bill Amount: ${formatCurrency(billAmount, selectedCurrency)}\n" +
                    "Tip Amount: $formattedTipAmount\n" +
                    "Total Bill Amount: $formattedTotalBill"
        }

        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Result")
        alertDialogBuilder.setMessage(resultMessage)
        alertDialogBuilder.setPositiveButton("OK") { _, _ -> }
        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }


    private fun showInvalidAmountPopup() {
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Invalid Amount")
        alertDialogBuilder.setMessage("Please enter a valid bill amount.")
        alertDialogBuilder.setPositiveButton("OK") { _, _ -> }
        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun resetInputFields() {
        billAmountEditText.text.clear()
        customTipEditText.text.clear()
        numberOfPeopleEditText.text.clear()
    }
}
