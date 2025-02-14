package com.hanif.eww

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hanif.eww.ui.theme.EWWTheme
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EWWTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "FirstScreen"
                ) {
                    composable(route = "FirstScreen") {
                        DynamicInputScreen(modifier = Modifier, onSubmit = {
                            navController.currentBackStackEntry?.savedStateHandle?.apply {
                                set("detailArgument", it)
                            }
                            navController.navigate("DetailScreen")
                        })
                    }
                    composable(route = "DetailScreen") {
                        val detailArgument =
                            navController.previousBackStackEntry?.savedStateHandle?.get<List<String>>(
                                "detailArgument"
                            )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            detailArgument?.let {
                                LazyColumn {
                                    itemsIndexed(detailArgument) { index, item ->
                                        Text("${index + 1}  = ${item}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicInputScreen(modifier: Modifier = Modifier, onSubmit: (List<String>) -> Unit) {
    var inputs by remember { mutableStateOf(listOf("")) }
    var touchedFields by remember { mutableStateOf(mutableSetOf<Int>()) }

    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            inputs = inputs.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
            touchedFields = touchedFields.map { index ->
                when {
                    index == from.index -> to.index
                    index in minOf(from.index, to.index)..maxOf(from.index, to.index) ->
                        if (from.index < to.index) index - 1 else index + 1
                    else -> index
                }
            }.toMutableSet()
        }
    )

    // Validation function for single input
    fun isValidInput(input: String): Boolean = input.length in 6..10

    // Validation function for all inputs
    fun areAllInputsValid(): Boolean = inputs.all { isValidInput(it) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text("Dynamic Input List")
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = {
                inputs = inputs + ""
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Add")
            }
            InputListComposable(
                inputs = inputs,
                touchedFields = touchedFields,
                onInputChange = { index, value ->
                    inputs = inputs.toMutableList().apply {
                        this[index] = value
                    }
                    touchedFields = touchedFields.toMutableSet().apply {
                        add(index)
                    }
                },
                onDelete = {
                    if (inputs.size > 1) {
                        inputs = inputs.toMutableList().apply {
                            removeAt(index = it)
                        }
                        touchedFields = touchedFields.filter { index -> index < it }.toMutableSet()
                    }
                },
                isValidInput = ::isValidInput,
                state = state,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    // Mark all fields as touched when submitting
                    touchedFields = inputs.indices.toMutableSet()
                    if (areAllInputsValid()) {
                        onSubmit(inputs)
                    }
                },
                enabled = areAllInputsValid(),  // Disable button if any input is invalid
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit")
            }
        }
    }
}

@Composable
fun InputListComposable(
    inputs: List<String>,
    touchedFields: Set<Int>,
    onInputChange: (Int, String) -> Unit,
    onDelete: (Int) -> Unit,
    isValidInput: (String) -> Boolean,
    state: ReorderableLazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .reorderable(state)
            .detectReorderAfterLongPress(state),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = state.listState
    ) {
        itemsIndexed(items = inputs) { index, item ->
            InputFieldComposable(
                value = item,
                onValueChange = { onInputChange(index, it) },
                onDelete = { onDelete(index) },
                isLastItem = index == inputs.lastIndex,
                showError = touchedFields.contains(index) && !isValidInput(item),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (state.draggingItemIndex != index) Modifier.padding(10.dp) else Modifier
                    )
            )
        }
    }
}

@Composable
fun InputFieldComposable(
    value: String,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit,
    isLastItem: Boolean,
    showError: Boolean,
    modifier: Modifier = Modifier
) {
    val focus = LocalFocusManager.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter the value between 6 to 10", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(
                    imeAction = if (isLastItem) ImeAction.Done else ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focus.clearFocus() }
                ),
                isError = showError,
                singleLine = true
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, "delete")
            }
        }
        if (showError) {
            Text(
                text = "Input must be 6 to 10 characters",
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }
    }
}