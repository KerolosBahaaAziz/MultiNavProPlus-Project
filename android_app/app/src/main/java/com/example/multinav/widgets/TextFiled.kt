package com.example.widgets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)


@Composable

fun CustomTextField(modifier: Modifier = Modifier,textFiledValue: MutableState<String>,placeHolder : String) {
    TextField(
        modifier = Modifier.fillMaxWidth(),
        value = textFiledValue.value,
        onValueChange = {
            newValue->
            textFiledValue.value = newValue },
        maxLines = 1,
        placeholder = {
            Text(
                text = placeHolder
            )
        },
        shape = RoundedCornerShape(8.dp),

    )

}