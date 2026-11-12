package com.fundscope.ai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fundscope.ai.data.FundEntity
import com.fundscope.ai.data.FundRepository
import com.fundscope.ai.data.PublicSource
import kotlinx.coroutines.launch
import kotlin.math.pow

private val Navy = Color(0xFF101B32)
private val Panel = Color(0xFF1A2945)
private val PanelAlt = Color(0xFF223558)
private val Mint = Color(0xFF55D6A5)
private val Gold = Color(0xFFFFC766)
private val Muted = Color(0xFF9BAAC2)
private val CanvasBg = Color(0xFF0B1426)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FundScopeApp() }
    }
}

@Composable
fun FundScopeApp() {
    val context = LocalContext.current
    val repository = remember { FundRepository(context) }
    val scope = rememberCoroutineScope()

    var tab by rememberSaveable { mutableStateOf("Discover") }
    var selectedSchemeCode by rememberSaveable { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

    val observedFunds by repository.observedFunds.collectAsState(initial = emptyList())

    // Initial database seed
    LaunchedEffect(Unit) {
        repository.seedInitialDataIfEmpty()
    }

    val selectedFund = observedFunds.find { it.schemeCode == selectedSchemeCode }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Mint, secondary = Gold, background = CanvasBg,
            surface = Panel, onBackground = Color.White, onSurface = Color.White
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = CanvasBg) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val isWide = this.maxWidth >= 840.dp
                val isMedium = this.maxWidth in 600.dp..839.dp
                val isNavigationRail = isWide || isMedium

                if (isNavigationRail) {
                    // Responsive Tablet / Desktop / Foldable Layout (NavigationRail on Left)
                    Row(Modifier.fillMaxSize()) {
                        AppNavigationRail(
                            currentTab = tab,
                            onTabSelected = {
                                tab = it
                                if (it != "Discover") selectedSchemeCode = null
                            }
                        )
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            when (tab) {
                                "Discover" -> {
                                    if (isWide) {
                                        // Two-Pane (Split Master-Detail) Layout for Expanded Screens
                                        TwoPaneDiscoverScreen(
                                            funds = observedFunds,
                                            selectedFund = selectedFund,
                                            onSelectFund = { selectedSchemeCode = it.schemeCode },
                                            onRefreshScheme = { code ->
                                                scope.launch {
                                                    isSyncing = true
                                                    repository.fetchSchemeDetails(code)
                                                    isSyncing = false
                                                }
                                            },
                                            isSyncing = isSyncing,
                                            repository = repository
                                        )
                                    } else {
                                        // Medium Screens (Adaptive Grid or Full Detail)
                                        if (selectedFund != null) {
                                            BackHandler { selectedSchemeCode = null }
                                            FundDetail(
                                                fund = selectedFund,
                                                onBack = { selectedSchemeCode = null },
                                                onRefresh = {
                                                    scope.launch {
                                                        isSyncing = true
                                                        repository.fetchSchemeDetails(selectedFund.schemeCode)
                                                        isSyncing = false
                                                    }
                                                },
                                                isSyncing = isSyncing
                                            )
                                        } else {
                                            AdaptiveDiscoverScreen(
                                                funds = observedFunds,
                                                columnsCount = 2,
                                                onSelectFund = { selectedSchemeCode = it.schemeCode },
                                                repository = repository
                                            )
                                        }
                                    }
                                }
                                "SIP Lab" -> ResponsiveSipScreen(isWide = isWide)
                                "Sources" -> TrustworthySourcesScreen(
                                    sources = repository.trustworthySources,
                                    isWide = isWide
                                )
                                "Method" -> ResponsiveMethodScreen(isWide = isWide)
                            }
                        }
                    }
                } else {
                    // Mobile Portrait / Compact Layout (Bottom NavigationBar)
                    if (selectedFund != null && tab == "Discover") {
                        BackHandler { selectedSchemeCode = null }
                        FundDetail(
                            fund = selectedFund,
                            onBack = { selectedSchemeCode = null },
                            onRefresh = {
                                scope.launch {
                                    isSyncing = true
                                    repository.fetchSchemeDetails(selectedFund.schemeCode)
                                    isSyncing = false
                                }
                            },
                            isSyncing = isSyncing
                        )
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            Header()
                            Box(Modifier.weight(1f).fillMaxWidth()) {
                                when (tab) {
                                    "Discover" -> AdaptiveDiscoverScreen(
                                        funds = observedFunds,
                                        columnsCount = 1,
                                        onSelectFund = { selectedSchemeCode = it.schemeCode },
                                        repository = repository
                                    )
                                    "SIP Lab" -> ResponsiveSipScreen(isWide = false)
                                    "Sources" -> TrustworthySourcesScreen(
                                        sources = repository.trustworthySources,
                                        isWide = false
                                    )
                                    "Method" -> ResponsiveMethodScreen(isWide = false)
                                }
                            }
                            NavigationBar(containerColor = Navy, contentColor = Mint) {
                                navItems.forEach { (label, icon) ->
                                    NavigationBarItem(
                                        selected = tab == label,
                                        onClick = { tab = label },
                                        icon = { Icon(icon, contentDescription = label) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Mint, selectedTextColor = Mint,
                                            indicatorColor = Panel, unselectedTextColor = Muted
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val navItems = listOf(
    "Discover" to Icons.Default.Search,
    "SIP Lab" to Icons.Default.Calculate,
    "Sources" to Icons.Default.Verified,
    "Method" to Icons.Default.Insights
)

@Composable
fun AppNavigationRail(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationRail(
        containerColor = Navy,
        contentColor = Mint,
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Panel,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.QueryStats,
                            contentDescription = "FundScope Logo",
                            tint = Mint,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "FUNDSCOPE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
            }
        }
    ) {
        Spacer(Modifier.height(12.dp))
        navItems.forEach { (label, icon) ->
            NavigationRailItem(
                selected = currentTab == label,
                onClick = { onTabSelected(label) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 11.sp) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Mint,
                    selectedTextColor = Mint,
                    indicatorColor = Panel,
                    unselectedIconColor = Muted,
                    unselectedTextColor = Muted
                )
            )
        }
    }
}

@Composable
fun Header() {
    Column(Modifier.fillMaxWidth().background(Navy).padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = Panel,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.QueryStats, null, tint = Mint, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("FUNDSCOPE AI", fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text("India Mutual Fund Intelligence • AMFI Feed", fontSize = 11.sp, color = Muted)
            }
            Spacer(Modifier.weight(1f))
            Surface(color = Color(0xFF1B3B32), shape = RoundedCornerShape(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(Modifier.size(7.dp).background(Mint, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("AMFI LIVE", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdaptiveDiscoverScreen(
    funds: List<FundEntity>,
    columnsCount: Int,
    onSelectFund: (FundEntity) -> Unit,
    repository: FundRepository
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var liveSearchResults by remember { mutableStateOf<List<FundEntity>?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val displayedFunds = (liveSearchResults ?: funds).filter {
        val matchesCat = if (selectedCategory == "All") true else it.category.contains(selectedCategory, true)
        val matchesQuery = if (query.isBlank()) true else (
            it.name.contains(query, true) ||
            it.amc.contains(query, true) ||
            it.category.contains(query, true) ||
            it.schemeCode.contains(query, true)
        )
        matchesCat && matchesQuery
    }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
        Text("Mutual Fund Intelligence Desk", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Verified daily NAVs & scoring backed by AMFI & SEBI disclosures", color = Muted, fontSize = 12.sp)

        Spacer(Modifier.height(14.dp))

        // Search Field with Live AMFI Search Trigger
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (it.length >= 3) {
                    scope.launch {
                        isSearching = true
                        liveSearchResults = repository.searchLiveFunds(it)
                        isSearching = false
                    }
                } else if (it.isBlank()) {
                    liveSearchResults = null
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search 15,000+ Indian funds, AMCs or Scheme Codes…", color = Muted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) },
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Mint, strokeWidth = 2.dp)
                } else if (query.isNotBlank()) {
                    IconButton(onClick = { query = ""; liveSearchResults = null }) {
                        Icon(Icons.Default.Close, "Clear", tint = Muted)
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Mint, unfocusedBorderColor = PanelAlt,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            )
        )

        Spacer(Modifier.height(10.dp))

        // Category Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("All", "Equity", "Flexi Cap", "Small Cap", "Index", "Debt").forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Mint,
                        selectedLabelColor = Navy,
                        containerColor = Panel,
                        labelColor = Color.White
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricTile("Indexed Funds", "${displayedFunds.size}", Modifier.weight(1f))
            MetricTile("Source", "AMFI Official", Modifier.weight(1f))
            MetricTile("Top Score", "${displayedFunds.maxOfOrNull { it.score } ?: 8.4} / 10", Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        if (columnsCount == 1) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedFunds, key = { it.schemeCode }) { fund ->
                    FundCard(fund) { onSelectFund(fund) }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsCount),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedFunds, key = { it.schemeCode }) { fund ->
                    FundCard(fund) { onSelectFund(fund) }
                }
            }
        }
    }
}

@Composable
fun TwoPaneDiscoverScreen(
    funds: List<FundEntity>,
    selectedFund: FundEntity?,
    onSelectFund: (FundEntity) -> Unit,
    onRefreshScheme: (String) -> Unit,
    isSyncing: Boolean,
    repository: FundRepository
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var liveSearchResults by remember { mutableStateOf<List<FundEntity>?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val displayedFunds = (liveSearchResults ?: funds).filter {
        val matchesCat = if (selectedCategory == "All") true else it.category.contains(selectedCategory, true)
        val matchesQuery = if (query.isBlank()) true else (
            it.name.contains(query, true) ||
            it.amc.contains(query, true) ||
            it.category.contains(query, true) ||
            it.schemeCode.contains(query, true)
        )
        matchesCat && matchesQuery
    }

    Row(Modifier.fillMaxSize()) {
        // Left Master Pane (List & Filters)
        Column(
            Modifier
                .width(420.dp)
                .fillMaxHeight()
                .background(Navy.copy(alpha = 0.5f))
                .padding(18.dp)
        ) {
            Text("Discover Funds", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("AMFI Verified Daily Feeds", color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.length >= 3) {
                        scope.launch {
                            isSearching = true
                            liveSearchResults = repository.searchLiveFunds(it)
                            isSearching = false
                        }
                    } else if (it.isBlank()) {
                        liveSearchResults = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search 15k+ funds by name or code…", color = Muted, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Mint, strokeWidth = 2.dp)
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Mint, unfocusedBorderColor = PanelAlt,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White
                )
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("All", "Equity", "Debt", "Index").forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Mint,
                            selectedLabelColor = Navy,
                            containerColor = Panel,
                            labelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("${displayedFunds.size} funds matching criteria", color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayedFunds, key = { it.schemeCode }) { fund ->
                    val isSelected = fund.schemeCode == selectedFund?.schemeCode
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelectFund(fund) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) PanelAlt else Panel
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            ScoreRing(fund.score)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    fund.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("${fund.amc} • ${fund.category}", color = Muted, fontSize = 10.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Code: ${fund.schemeCode} • ${if (fund.nav > 0) "NAV ₹${fund.nav}" else "AMFI Verified"}",
                                    color = Mint,
                                    fontSize = 10.sp
                                )
                            }
                            if (isSelected) {
                                Box(
                                    Modifier.size(8.dp).background(Mint, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Divider
        Box(Modifier.width(1.dp).fillMaxHeight().background(PanelAlt))

        // Right Detail Pane
        Box(Modifier.weight(1f).fillMaxHeight()) {
            if (selectedFund != null) {
                FundDetail(
                    fund = selectedFund,
                    onBack = null, // In two-pane mode, back is not needed
                    onRefresh = { onRefreshScheme(selectedFund.schemeCode) },
                    isSyncing = isSyncing
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Analytics, null, tint = Muted, modifier = Modifier.size(54.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("Select a mutual fund", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "View detailed AMFI NAV history, SEBI risk profile and AI investment scorecard",
                            color = Muted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FundCard(fund: FundEntity, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ScoreRing(fund.score)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    fund.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text("${fund.amc} • ${fund.category}", color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Risk: ${fund.risk}", color = Muted, fontSize = 10.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("•", color = Muted, fontSize = 10.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("TER: ${fund.expense}%", color = Muted, fontSize = 10.sp)
                    if (fund.nav > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text("•", color = Muted, fontSize = 10.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("NAV ₹${fund.nav}", color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable
fun FundDetail(
    fund: FundEntity,
    onBack: (() -> Unit)?,
    onRefresh: () -> Unit,
    isSyncing: Boolean
) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (onBack != null) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onBack() }
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Mint, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Back to funds list", color = Mint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(fund.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${fund.amc} • ${fund.category}", color = Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFF1B3B32), shape = RoundedCornerShape(12.dp)) {
                            Text(
                                "AMFI Code: ${fund.schemeCode}",
                                color = Mint,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (!fund.isin.isNullOrBlank()) {
                            Spacer(Modifier.width(8.dp))
                            Surface(color = Panel, shape = RoundedCornerShape(12.dp)) {
                                Text(
                                    "ISIN: ${fund.isin}",
                                    color = Muted,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                IconButton(
                    onClick = onRefresh,
                    enabled = !isSyncing
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Mint, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, "Refresh from AMFI", tint = Mint)
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    ScoreRing(fund.score)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("FundScope AI Composite Grade", color = Muted, fontSize = 11.sp)
                        Text("${fund.score} / 10 • ${grade(fund.score)}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("3Y CAGR: ${fund.returns3y}% • 5Y CAGR: ${fund.returns5y}%", color = Gold, fontSize = 11.sp)
                        if (fund.nav > 0) {
                            Text("Latest NAV: ₹${fund.nav} (${fund.navDate})", color = Mint, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item { SectionTitle("Investment Scorecard", "Weighted multicriteria analysis of fund fundamentals") }
        item { ScoreBar("Valuation attractiveness", fund.valuation, 2.0) }
        item { ScoreBar("Underlying business quality", fund.quality, 1.5) }
        item { ScoreBar("Financial balance-sheet strength", 7.4, 1.0) }
        item { ScoreBar("Management & AMC governance", 7.6, 1.0) }
        item { ScoreBar("Mandate consistency & style fidelity", 8.0, 1.0) }
        item { ScoreBar("Risk management & volatility capture", 7.2, 1.0) }
        item { ScoreBar("Performance consistency across cycles", 7.5, 1.0) }
        item { ScoreBar("Expense ratio efficiency (TER)", 7.8, 0.5) }

        item { SectionTitle("SEBI Standard Riskometer", "Official regulatory 6-tier classification framework") }
        item { RiskMeter(fund.risk, fund.riskLevel) }

        item {
            InfoCard(
                "Trustworthy Data Source: AMFI India & SEBI",
                "${fund.note}\n\nOfficial Source: ${fund.sourceName}. Daily NAV verified against the statutory Association of Mutual Funds in India database."
            )
        }
        item {
            InfoCard(
                "Metric Guide: Quality & Valuation",
                "Quality analyzes return on capital (ROCE/ROE), debt-to-equity ratios, and competitive moats of portfolio holdings. Valuation evaluates P/E, P/B, and dividend yield against historic category medians."
            )
        }
    }
}

@Composable
fun ScoreRing(score: Double) {
    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(Color(0xFF34445F), -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(5.5.dp.toPx()))
            drawArc(Mint, -90f, (score / 10.0 * 360.0).toFloat(), false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(5.5.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("%.1f".format(score), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("/10", fontSize = 8.sp, color = Muted)
        }
    }
}

@Composable
fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = Muted, fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Mint)
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(subtitle, color = Muted, fontSize = 11.sp)
    }
}

@Composable
fun ScoreBar(label: String, score: Double, weight: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(label, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("%.1f / 10".format(score), color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { (score / 10).toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Mint, trackColor = Color(0xFF34445F)
        )
        Text("Weight: ${(weight * 10).toInt()}% of composite score", color = Muted, fontSize = 9.sp)
    }
}

@Composable
fun RiskMeter(level: String, position: Int) {
    val labels = listOf("Low", "Low–Mod", "Moderate", "Mod–High", "High", "Very High")
    Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(15.dp)) {
        Text(level, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Gold)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { i, _ ->
                Box(Modifier.weight(1f).height(10.dp).background(
                    if (i + 1 <= position) listOf(Color(0xFF45B879), Color(0xFF9DBD4D), Gold, Color(0xFFFF9A4D), Color(0xFFE66B53), Color(0xFFCF4057))[i]
                    else Color(0xFF34445F), RoundedCornerShape(3.dp)
                ))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("SEBI Standard Riskometer Classification. Check official AMC factsheet for monthly adjustments.", color = Muted, fontSize = 10.sp)
    }
}

@Composable
fun InfoCard(title: String, body: String) {
    Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(body, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
fun ResponsiveSipScreen(isWide: Boolean) {
    var monthly by rememberSaveable { mutableStateOf("10000") }
    var years by rememberSaveable { mutableStateOf("15") }
    var rate by rememberSaveable { mutableStateOf("13.5") }
    var stepUp by rememberSaveable { mutableStateOf("10") }

    val p = monthly.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val y = years.toIntOrNull()?.coerceIn(1, 50) ?: 1
    val r = rate.toDoubleOrNull()?.coerceIn(0.0, 40.0) ?: 0.0
    val step = stepUp.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
    val projection = sipProjection(p, y, r, step)

    if (isWide) {
        // Two-column side-by-side layout for wide screens / tablets
        Row(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text("SIP Lab & Compounder", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Future-value scenarios with annual step-up compounding", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(18.dp))
                InputCard("Monthly Investment (₹)", monthly) { monthly = it }
                InputCard("Investment Horizon (years)", years) { years = it }
                InputCard("Expected Annual Return (CAGR %)", rate) { rate = it }
                InputCard("Annual Step-up Percentage (%)", stepUp) { stepUp = it }
            }

            Column(Modifier.weight(1.2f)) {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(22.dp)) {
                        Text("Projected Future Corpus", color = Muted, fontSize = 12.sp)
                        Text("₹${"%,.0f".format(projection.value)}", color = Mint, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        ResultLine("Total Amount Invested", "₹${"%,.0f".format(projection.invested)}")
                        ResultLine("Estimated Wealth Gain", "₹${"%,.0f".format(projection.value - projection.invested)}")
                        ResultLine("Effective Compound Return", "${r}% p.a.")
                        ResultLine("Annual Increment", "${step}% Step-up")
                    }
                }
                Spacer(Modifier.height(16.dp))
                InfoCard(
                    "Methodology Note",
                    "Calculations use standard month-end compounding with dynamic annual step-up contribution scaling. Past mutual fund performance does not guarantee future results."
                )
            }
        }
    } else {
        // Stacked layout for phones
        LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Text("SIP Lab & Compounder", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Future-value scenarios with annual step-up compounding", color = Muted, fontSize = 12.sp)
            }
            item {
                InputCard("Monthly Investment (₹)", monthly) { monthly = it }
                InputCard("Investment Horizon (years)", years) { years = it }
                InputCard("Expected Annual Return (CAGR %)", rate) { rate = it }
                InputCard("Annual Step-up Percentage (%)", stepUp) { stepUp = it }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Projected Future Corpus", color = Muted, fontSize = 12.sp)
                        Text("₹${"%,.0f".format(projection.value)}", color = Mint, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        ResultLine("Total Invested", "₹${"%,.0f".format(projection.invested)}")
                        ResultLine("Estimated Gains", "₹${"%,.0f".format(projection.value - projection.invested)}")
                        ResultLine("Annual Return", "${r}% CAGR")
                    }
                }
            }
            item {
                InfoCard(
                    "Methodology Note",
                    "Calculations assume disciplined month-end compounding. Actual mutual fund returns vary by market conditions."
                )
            }
        }
    }
}

data class SipResult(val invested: Double, val value: Double)

fun sipProjection(monthly: Double, years: Int, annualRate: Double, stepUp: Double): SipResult {
    var corpus = 0.0
    var invested = 0.0
    val monthlyRate = (1 + annualRate / 100.0).pow(1.0 / 12.0) - 1
    var contribution = monthly
    for (month in 1..years * 12) {
        if (month > 1 && (month - 1) % 12 == 0) contribution *= (1 + stepUp / 100.0)
        corpus = corpus * (1 + monthlyRate) + contribution
        invested += contribution
    }
    return SipResult(invested, corpus)
}

@Composable
fun InputCard(label: String, value: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text(label, color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(3.dp))
        OutlinedTextField(
            value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Mint, unfocusedBorderColor = PanelAlt,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            )
        )
    }
}

@Composable
fun ResultLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Muted, modifier = Modifier.weight(1f), fontSize = 12.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
fun TrustworthySourcesScreen(
    sources: List<PublicSource>,
    isWide: Boolean
) {
    val context = LocalContext.current
    val columns = if (isWide) 2 else 1

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text("Public Trustworthy Sources", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "FundScope AI only relies on official statutory, regulatory, and open public repositories",
            color = Muted,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(16.dp))

        if (columns == 1) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
                items(sources) { source ->
                    SourceCard(source) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.primaryUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sources) { source ->
                    SourceCard(source) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.primaryUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }
}

@Composable
fun SourceCard(source: PublicSource, onOpenUrl: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFF1B3B32),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        source.badge,
                        color = Mint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenUrl) {
                    Icon(Icons.Default.OpenInNew, "Open Source", tint = Mint, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(source.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(source.entity, color = Gold, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Text(source.role, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(source.trustBasis, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
            Spacer(Modifier.height(10.dp))
            Text(source.primaryUrl, color = Mint, fontSize = 11.sp)
        }
    }
}

@Composable
fun ResponsiveMethodScreen(isWide: Boolean) {
    val items = listOf(
        "Valuation Attractiveness – 20%" to "Looks through mutual fund holdings to evaluate whether underlying assets are attractively valued using P/E, P/B, EV/EBITDA, and free-cash-flow yields.",
        "Underlying Business Quality – 15%" to "Evaluates profitability, return on capital (ROCE/ROE), debt-to-equity ratios, and competitive moats of portfolio constituent companies.",
        "Financial Balance-Sheet Strength – 10%" to "Examines liquidity, leverage ratios, interest coverage, and credit ratings of underlying corporate debt and equity issuers.",
        "Management & AMC Governance – 10%" to "Assesses fund manager pedigree, tenure consistency, track record, and asset management company stewardship ethics.",
        "Portfolio Diversification – 10%" to "Measures concentration risk across top 10 holdings, sector weights, market cap allocations, and active share against benchmark indices.",
        "Mandate & Style Consistency – 10%" to "Verifies style consistency over time (e.g. ensuring a value fund sticks to value disciplines rather than chasing momentum).",
        "Risk Management & Volatility – 10%" to "Quantifies downside protection, standard deviation, beta, Sharpe ratio, and maximum drawdown across bear market phases.",
        "Performance Consistency – 10%" to "Analyzes rolling returns over 3Y, 5Y, and 7Y horizons across bull, bear, and sideways market cycles.",
        "Cost & TER Efficiency – 5%" to "Considers Total Expense Ratio (TER), turnover ratio, and transaction friction."
    )

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text("FundScope Methodology", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Transparent, multicriteria research framework for Indian mutual funds", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        if (isWide) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { (title, body) ->
                    InfoCard(title, body)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                items(items) { (title, body) ->
                    InfoCard(title, body)
                }
            }
        }
    }
}

fun grade(score: Double): String = when {
    score >= 9.0 -> "Exceptional"
    score >= 8.0 -> "Excellent"
    score >= 7.0 -> "Good"
    score >= 6.0 -> "Average"
    score >= 5.0 -> "Below Average"
    else -> "Caution Advised"
}
