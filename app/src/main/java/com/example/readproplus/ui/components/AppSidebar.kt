package com.example.readproplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.readproplus.model.SidebarSection
import com.example.readproplus.ui.theme.ReaderColorScheme

@Composable
fun AppSidebar(
    currentSection: SidebarSection,
    onSectionSelected: (SidebarSection) -> Unit,
    scheme: ReaderColorScheme,
    onNewUiModeClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(scheme.surfaceColor)
            .padding(top = 48.dp, bottom = 16.dp),
    ) {
        Text(
            text = "ReadProPlus",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.accentColor,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )

        HorizontalDivider(
            color = scheme.dividerColor.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
        ) {
            SidebarSection.entries.forEach { section ->
                SidebarItem(
                    section = section,
                    isSelected = section == currentSection,
                    onClick = { onSectionSelected(section) },
                    scheme = scheme,
                )
            }
        }

        HorizontalDivider(
            color = scheme.dividerColor.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.accentColor.copy(alpha = 0.10f))
                .clickable(onClick = onNewUiModeClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.ViewModule,
                contentDescription = "Open new UI mode",
                tint = scheme.accentColor,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = "New UI mode",
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.accentColor,
                    fontSize = 15.sp,
                )
                Text(
                    text = "Try the redesigned flow",
                    color = scheme.pageNumberColor,
                    fontSize = 11.sp,
                )
            }
        }

        Text(
            text = "v1.0.3",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.pageNumberColor.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SidebarItem(
    section: SidebarSection,
    isSelected: Boolean,
    onClick: () -> Unit,
    scheme: ReaderColorScheme,
) {
    val bgColor = if (isSelected) scheme.accentColor.copy(alpha = 0.15f) else scheme.surfaceColor
    val textColor = if (isSelected) scheme.accentColor else scheme.navigationContent
    val iconColor = if (isSelected) scheme.accentColor else scheme.pageNumberColor

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = section.icon,
            contentDescription = section.label,
            tint = iconColor,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = section.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            fontSize = 15.sp,
        )
    }
}
