package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.QuickSite
import com.example.model.SearchEngine
import com.example.model.SiteCatalog
import com.example.model.SiteCategory
import com.example.ui.theme.OrbitBlue
import com.example.ui.theme.OrbitCyan
import com.example.ui.theme.OrbitEmerald
import com.example.ui.theme.OrbitViolet

@Composable
fun BrowserStartPage(
    isIncognito: Boolean,
    selectedEngine: SearchEngine,
    onOpenUrl: (String) -> Unit,
    onOpenEnginePicker: () -> Unit,
    onOpenAiAssistant: (String?) -> Unit
) {
    var filterQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(SiteCategory.ALL) }

    val filteredSites by remember(filterQuery, selectedCategory) {
        derivedStateOf {
            val q = filterQuery.trim().lowercase()
            SiteCatalog.sites.filter { site ->
                val matchesCat = selectedCategory == SiteCategory.ALL || site.category == selectedCategory
                val matchesQuery = q.isEmpty() ||
                    site.name.lowercase().contains(q) ||
                    site.url.lowercase().contains(q) ||
                    site.tagline.lowercase().contains(q) ||
                    site.category.label.lowercase().contains(q) ||
                    site.region.lowercase().contains(q)
                matchesCat && matchesQuery
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 152.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isIncognito) Color(0xFF120C22) else MaterialTheme.colorScheme.background
            )
    ) {
        // Hero Banner
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.height(132.dp).fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.orbit_hero_banner_1795308854033),
                        contentDescription = "Orbit Browser start page hero",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        if (isIncognito) Color(0xDD1A0F33) else Color(0xDD070B16),
                                        Color(0x66070B16)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isIncognito) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Incognito",
                                        tint = OrbitViolet,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (isIncognito) "Incognito Orbit" else "Orbit Global Start",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clickable { onOpenEnginePicker() }
                                    .testTag("start_engine_chip")
                            ) {
                                Text(
                                    text = "Engine: ${selectedEngine.displayName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = OrbitCyan,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isIncognito) {
                                "History and cookies will not be saved in this tab."
                            } else {
                                "${SiteCatalog.sites.size} curated international websites • Instant filter & search"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }

        // Smart Search / Site Filter Box
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = { Text("Search ${SiteCatalog.sites.size} sites or press Go for ${selectedEngine.displayName}...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Filter or search") },
                trailingIcon = {
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { filterQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (filterQuery.isNotBlank()) onOpenUrl(filterQuery)
                    }
                ),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isIncognito) OrbitViolet else OrbitCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_page_smart_search")
            )
        }

        // Ask Orbit AI Quick Card
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAiAssistant(filterQuery.takeIf { it.isNotBlank() }) }
                    .testTag("start_ask_ai_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(OrbitCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Orbit AI",
                            tint = OrbitCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (filterQuery.isNotBlank()) {
                                "Ask Orbit AI: \"$filterQuery\""
                            } else {
                                "Ask Orbit AI Anything"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "যেকোনো প্রশ্ন করুন — বাংলা বা ইংরেজিতে সঠিক ও দ্রুত উত্তর পান",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 13 Categories + All
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SiteCategory.entries) { category ->
                    val selected = category == selectedCategory
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategory = category },
                        label = { Text(category.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isIncognito) OrbitViolet else OrbitCyan,
                            selectedLabelColor = Color(0xFF070B16)
                        ),
                        modifier = Modifier.testTag("category_chip_${category.name}")
                    )
                }
            }
        }

        // Filtered Count Header
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "${selectedCategory.label} (${filteredSites.size} sites)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Sites Grid
        items(filteredSites, key = { "${it.category.name}_${it.name}_${it.url}" }) { site ->
            QuickSiteCard(
                site = site,
                onClick = { onOpenUrl(site.url) }
            )
        }
    }
}

@Composable
private fun QuickSiteCard(
    site: QuickSite,
    onClick: () -> Unit
) {
    val badgeColor = remember(site.category) {
        when (site.category) {
            SiteCategory.SEARCH, SiteCategory.GOOGLE_CHROME -> OrbitCyan
            SiteCategory.AI -> OrbitViolet
            SiteCategory.FINANCE, SiteCategory.LEARNING -> OrbitEmerald
            else -> OrbitBlue
        }
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag("site_card_${site.name}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(badgeColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = site.badge,
                        style = MaterialTheme.typography.labelLarge,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = site.region,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = site.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = site.tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
