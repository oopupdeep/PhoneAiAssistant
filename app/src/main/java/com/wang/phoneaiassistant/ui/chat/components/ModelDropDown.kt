package com.wang.phoneaiassistant.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wang.phoneaiassistant.ui.chat.ChatViewModel


@Composable
fun ModelDropdowns(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel()
) {
    val selectedModel by viewModel.selectedModel
    val models by viewModel.models.observeAsState(initial = emptyList())

    val selectedCompany by viewModel.selectedCompany
    val companies by viewModel.companies.observeAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        viewModel.loadCompanies()
        viewModel.loadModels()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        // 公司选择下拉菜单
        StyledDropdownMenu(
            displayText = selectedCompany,
            items = companies,
            onItemSelected = { company -> viewModel.onCompanySelected(company) }
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 模型选择下拉菜单
        StyledDropdownMenu(
            displayText = selectedModel.id,
            items = models.map { it.id }, // 我们只需要模型的ID字符串列表
            onItemSelected = { modelId ->
                // 根据ID找到完整的ModelInfo对象
                models.find { it.id == modelId }?.let {
                    viewModel.onModelSelected(it)
                }
            }
        )
    }
}


@Composable
private fun <T> StyledDropdownMenu(
    displayText: String,
    items: List<T>,
    onItemSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var buttonWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier
                .height(48.dp)
                .onSizeChanged {
                    buttonWidth = with(density) { it.width.toDp() }
                },
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text(
                text = displayText, 
                fontSize = 16.sp, 
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "展开",
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(buttonWidth)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.toString()) }, // 确保 T 类型可以转为字符串显示
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}