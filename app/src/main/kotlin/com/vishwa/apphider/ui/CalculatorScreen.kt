package com.vishwa.apphider.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong

/**
 * Calculator-shaped cover for the disguised launcher icon. Entering the App Hider PIN as a bare
 * number and pressing equals opens the real UI; the dialer secret code works as well.
 */
@Composable
fun CalculatorScreen(onUnlock: (String) -> Boolean) {
    var display by remember { mutableStateOf("0") }
    var stored by remember { mutableStateOf<Double?>(null) }
    var pending by remember { mutableStateOf<String?>(null) }
    var waiting by remember { mutableStateOf(false) }
    var invalid by remember { mutableStateOf(false) }

    fun enterDigit(digit: String) {
        display = when {
            invalid || waiting -> digit.also {
                waiting = false
                invalid = false
            }
            display == "0" && digit != "0" -> digit
            display.length >= 12 -> display
            else -> display + digit
        }
    }

    fun chooseOperation(operation: String) {
        val current = display.toDoubleOrNull() ?: return
        val previous = stored
        val operationPending = pending
        if (previous != null && operationPending != null && !waiting) {
            val result = calculate(previous, current, operationPending)
            if (result == null) {
                invalid = true
                display = "Cannot divide by zero"
                return
            }
            display = format(result)
            stored = result
        } else {
            stored = current
        }
        pending = operation
        waiting = true
        invalid = false
    }

    fun pressEquals() {
        val previous = stored
        val operation = pending
        if (previous != null && operation != null && !waiting) {
            val result = calculate(previous, display.toDoubleOrNull() ?: 0.0, operation)
            if (result == null) {
                invalid = true
                display = "Cannot divide by zero"
                return
            }
            display = format(result)
            stored = null
            pending = null
            waiting = true
            invalid = false
            return
        }

        if (!onUnlock(display)) {
            invalid = true
            display = "0"
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = display,
                modifier = Modifier.fillMaxWidth().height(96.dp),
                textAlign = TextAlign.End,
                maxLines = 1,
                style = MaterialTheme.typography.displayMedium,
                color = if (invalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )

            val rows = listOf(
                listOf("C", "⌫", "±", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "−"),
                listOf("1", "2", "3", "+"),
            )
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { label ->
                        CalculatorButton(
                            label = label,
                            modifier = Modifier.weight(1f),
                            onClick = { handleAction(label) { digit, operation ->
                                when {
                                    digit != null -> enterDigit(digit)
                                    operation != null -> chooseOperation(operation)
                                    label == "C" -> {
                                        display = "0"
                                        stored = null
                                        pending = null
                                        waiting = false
                                        invalid = false
                                    }
                                    label == "⌫" && !invalid -> {
                                        display = if (display.length <= 1) "0" else display.dropLast(1)
                                    }
                                    label == "±" && !invalid -> {
                                        display = when {
                                            display.startsWith("-") -> display.removePrefix("-")
                                            display == "0" -> display
                                            else -> "-$display"
                                        }
                                    }
                                }
                            } },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CalculatorButton(label = "0", modifier = Modifier.weight(2f), onClick = { enterDigit("0") })
                CalculatorButton(label = ".", modifier = Modifier.weight(1f), onClick = {
                    if (invalid || waiting) {
                        display = "0."
                        waiting = false
                        invalid = false
                    } else if (!display.contains('.')) {
                        display += "."
                    }
                })
                CalculatorButton(label = "=", modifier = Modifier.weight(1f), onClick = { pressEquals() })
            }
        }
    }
}

@Composable
private fun CalculatorButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(24.dp),
            maxLines = 1,
        )
    }
}

private fun handleAction(label: String, action: (String?, String?) -> Unit) {
    when (label) {
        in listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9") -> action(label, null)
        "÷", "×", "−", "+" -> action(null, label)
        else -> action(null, null)
    }
}

private fun calculate(first: Double, second: Double, operation: String): Double? =
    when (operation) {
        "+" -> first + second
        "−" -> first - second
        "×" -> first * second
        "÷" -> if (second == 0.0) null else first / second
        else -> second
    }

private fun format(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "0"
    if (value % 1.0 == 0.0) return value.roundToLong().toString()
    return ((value * 1_000_000).roundToLong() / 1_000_000.0).toString()
}
