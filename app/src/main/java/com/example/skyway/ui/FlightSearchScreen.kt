package com.example.skyway.ui

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.skyway.R
import com.example.skyway.ui.theme.LocalPlaceholderNotifier
import com.example.skyway.ui.theme.PlaceholderNotifier
import com.example.skyway.ui.theme.SkyWayTheme
import com.example.skyway.ui.theme.snackbar
import com.example.skyway.ui.theme.spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val cityResources = listOf(
    R.string.city_moscow, R.string.city_petersburg, R.string.city_sochi,
    R.string.city_istanbul, R.string.city_dubai
)

/**
 * The screen owns primitive, saveable form values. Selected cities are resource indices,
 * not translated strings, so a language change also translates the existing selection.
 * Custom cities remain exactly as entered. No network requests or bookings are simulated.
 */
@Composable
fun FlightSearchScreen(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val notifier = MaterialTheme.snackbar
    val focusManager = LocalFocusManager.current
    val today = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    var originIndex by rememberSaveable { mutableIntStateOf(0) }
    var destinationIndex by rememberSaveable { mutableIntStateOf(2) }
    var originCustom by rememberSaveable { mutableStateOf("") }
    var destinationCustom by rememberSaveable { mutableStateOf("") }
    var roundTrip by rememberSaveable { mutableStateOf(true) }
    var departure by rememberSaveable { mutableLongStateOf(today + 7 * MILLIS_PER_DAY) }
    var returnDate by rememberSaveable { mutableLongStateOf(today + 14 * MILLIS_PER_DAY) }
    var passengers by rememberSaveable { mutableIntStateOf(1) }
    var cabin by rememberSaveable { mutableIntStateOf(0) }
    var directOnly by rememberSaveable { mutableStateOf(false) }
    var dateDialog by rememberSaveable { mutableIntStateOf(0) }
    var validationRequested by rememberSaveable { mutableStateOf(false) }

    val cities = cityResources.map { stringResource(it) }
    val origin = cities.getOrNull(originIndex) ?: originCustom
    val destination = cities.getOrNull(destinationIndex) ?: destinationCustom
    val sameCity = origin.isNotBlank() && origin.trim().equals(destination.trim(), ignoreCase = true)
    val originError = if (validationRequested && origin.isBlank()) R.string.error_origin_required else null
    val destinationError = when {
        !validationRequested -> null
        destination.isBlank() -> R.string.error_destination_required
        sameCity -> R.string.error_same_city
        else -> null
    }
    val invalidDates = departure < today || (roundTrip && returnDate < departure)
    val cabinLabels = listOf(
        stringResource(R.string.class_economy),
        stringResource(R.string.class_business),
        stringResource(R.string.class_first)
    )

    Column(modifier = modifier.fillMaxSize()) {
        BrandBar()
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= 840.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (wide) spacing.xl else spacing.md, vertical = spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.lg)
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 1120.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg)
                ) {
                    JourneyHero()
                    val routeContent: @Composable () -> Unit = {
                        RouteCard(
                            origin = origin,
                            destination = destination,
                            cities = cities,
                            roundTrip = roundTrip,
                            departure = departure,
                            returnDate = returnDate,
                            originError = originError,
                            destinationError = destinationError,
                            dateError = validationRequested && invalidDates,
                            onTripChanged = { roundTrip = it },
                            onOriginChanged = { originIndex = -1; originCustom = it },
                            onDestinationChanged = { destinationIndex = -1; destinationCustom = it },
                            onOriginSelected = { originIndex = it },
                            onDestinationSelected = { destinationIndex = it },
                            onSwap = {
                                val oldIndex = originIndex
                                val oldCustom = originCustom
                                originIndex = destinationIndex
                                originCustom = destinationCustom
                                destinationIndex = oldIndex
                                destinationCustom = oldCustom
                            },
                            onDepartureClick = { focusManager.clearFocus(); dateDialog = 1 },
                            onReturnClick = { focusManager.clearFocus(); dateDialog = 2 }
                        )
                    }
                    val preferencesContent: @Composable () -> Unit = {
                        PreferencesCard(
                            passengers = passengers,
                            cabin = cabin,
                            cabinLabels = cabinLabels,
                            directOnly = directOnly,
                            onPassengersChanged = { passengers = it.coerceIn(1, 9) },
                            onCabinChanged = { cabin = it },
                            onDirectChanged = { directOnly = it }
                        )
                    }
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                            Box(Modifier.weight(1.1f)) { routeContent() }
                            Box(Modifier.weight(1f)) { preferencesContent() }
                        }
                    } else {
                        routeContent()
                        preferencesContent()
                    }
                    SearchSummary(
                        origin = origin,
                        destination = destination,
                        passengerSummary = pluralStringResource(R.plurals.passenger_count, passengers, passengers),
                        cabinLabel = cabinLabels[cabin],
                        directOnly = directOnly,
                        onSearch = {
                            focusManager.clearFocus()
                            validationRequested = true
                            if (origin.isNotBlank() && destination.isNotBlank() && !sameCity && !invalidDates) {
                                notifier.notifyPlaceholder()
                            }
                        }
                    )
                }
                Spacer(Modifier.height(spacing.xs))
            }
        }
        NavigationBar {
            NavigationBarItem(
                selected = true,
                onClick = { focusManager.clearFocus() },
                icon = { Icon(SkyIcons.Search, contentDescription = null) },
                label = { Text(stringResource(R.string.nav_search)) }
            )
            NavigationBarItem(
                selected = false,
                onClick = notifier::notifyPlaceholder,
                icon = { Icon(SkyIcons.Heart, contentDescription = null) },
                label = { Text(stringResource(R.string.nav_favorites)) },
                modifier = Modifier.testTag("nav_favorites")
            )
            NavigationBarItem(
                selected = false,
                onClick = notifier::notifyPlaceholder,
                icon = { Icon(SkyIcons.Ticket, contentDescription = null) },
                label = { Text(stringResource(R.string.nav_tickets)) },
                modifier = Modifier.testTag("nav_tickets")
            )
        }
    }
    if (dateDialog != 0) {
        TravelDateDialog(
            initialDate = if (dateDialog == 1) departure else returnDate,
            minimumDate = if (dateDialog == 1) today else maxOf(today, departure),
            onDismiss = { dateDialog = 0 },
            onConfirm = { selectedDate ->
                if (dateDialog == 1) {
                    departure = selectedDate
                    if (returnDate < selectedDate) returnDate = selectedDate
                } else {
                    returnDate = selectedDate
                }
                dateDialog = 0
            }
        )
    }
}

@Composable
private fun BrandBar() {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(SkyIcons.Plane, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
        Column {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.brand_tagline),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun JourneyHero() {
    val spacing = MaterialTheme.spacing
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(colors.onPrimaryContainer.copy(alpha = 0.045f), radius = size.width * .25f,
                    center = Offset(size.width * .97f, size.height * .2f))
                drawCircle(colors.onPrimaryContainer.copy(alpha = 0.045f), radius = size.width * .15f,
                    center = Offset(size.width * .95f, size.height * .25f))
                drawLine(
                    color = colors.onPrimaryContainer.copy(alpha = .16f),
                    start = Offset(size.width * .65f, size.height * .95f),
                    end = Offset(size.width * .99f, size.height * .12f),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 8.dp.toPx()))
                )
            }
            Column(
                modifier = Modifier.padding(spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Text(stringResource(R.string.hero_eyebrow), style = MaterialTheme.typography.labelSmall)
                Text(
                    stringResource(R.string.hero_title),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() }
                )
                Text(stringResource(R.string.hero_subtitle), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteCard(
    origin: String,
    destination: String,
    cities: List<String>,
    roundTrip: Boolean,
    departure: Long,
    returnDate: Long,
    @StringRes originError: Int?,
    @StringRes destinationError: Int?,
    dateError: Boolean,
    onTripChanged: (Boolean) -> Unit,
    onOriginChanged: (String) -> Unit,
    onDestinationChanged: (String) -> Unit,
    onOriginSelected: (Int) -> Unit,
    onDestinationSelected: (Int) -> Unit,
    onSwap: () -> Unit,
    onDepartureClick: () -> Unit,
    onReturnClick: () -> Unit
) {
    val spacing = MaterialTheme.spacing
    SectionCard {
        SectionHeading(R.string.route_title, R.string.route_subtitle)
        PrimaryTabRow(selectedTabIndex = if (roundTrip) 0 else 1) {
            Tab(selected = roundTrip, onClick = { onTripChanged(true) },
                text = { Text(stringResource(R.string.round_trip)) }, modifier = Modifier.testTag("round_trip"))
            Tab(selected = !roundTrip, onClick = { onTripChanged(false) },
                text = { Text(stringResource(R.string.one_way)) }, modifier = Modifier.testTag("one_way"))
        }
        Column {
            CityField(
                value = origin, label = R.string.origin_label, cities = cities,
                onValueChanged = onOriginChanged, onCitySelected = onOriginSelected,
                error = originError, tag = "origin"
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                FilledTonalIconButton(onClick = onSwap, modifier = Modifier.testTag("swap_route")) {
                    Icon(SkyIcons.Swap, contentDescription = stringResource(R.string.swap_route))
                }
            }
            CityField(
                value = destination, label = R.string.destination_label, cities = cities,
                onValueChanged = onDestinationChanged, onCitySelected = onDestinationSelected,
                error = destinationError, tag = "destination"
            )
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth().animateContentSize(), verticalAlignment = Alignment.CenterVertically) {
            DateButton(
                label = R.string.departure_label, date = departure, onClick = onDepartureClick,
                modifier = Modifier.weight(1f).testTag("departure_date")
            )
            if (roundTrip) {
                VerticalDivider(modifier = Modifier.height(48.dp).padding(horizontal = spacing.sm))
                DateButton(
                    label = R.string.return_label, date = returnDate, onClick = onReturnClick,
                    modifier = Modifier.weight(1f).testTag("return_date")
                )
            }
        }
        AnimatedVisibility(visible = dateError) {
            Text(stringResource(R.string.error_dates), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityField(
    value: String,
    @StringRes label: Int,
    cities: List<String>,
    onValueChanged: (String) -> Unit,
    onCitySelected: (Int) -> Unit,
    @StringRes error: Int?,
    tag: String
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChanged(it); expanded = true },
            label = { Text(stringResource(label)) },
            placeholder = { Text(stringResource(R.string.city_placeholder)) },
            singleLine = true,
            leadingIcon = { Icon(SkyIcons.Pin, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = error != null,
            supportingText = if (error != null) ({ Text(stringResource(error)) }) else null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .testTag(tag)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            cities.forEachIndexed { index, city ->
                DropdownMenuItem(
                    text = { Text(city) },
                    onClick = { onCitySelected(index); expanded = false },
                    leadingIcon = { Icon(SkyIcons.Pin, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
private fun DateButton(@StringRes label: Int, date: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val formatted = remember(date, locale) {
        Instant.ofEpochMilli(date).atOffset(ZoneOffset.UTC).toLocalDate()
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }
    Surface(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.sm).heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            Icon(SkyIcons.Calendar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(stringResource(label), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatted, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun PreferencesCard(
    passengers: Int,
    cabin: Int,
    cabinLabels: List<String>,
    directOnly: Boolean,
    onPassengersChanged: (Int) -> Unit,
    onCabinChanged: (Int) -> Unit,
    onDirectChanged: (Boolean) -> Unit
) {
    val spacing = MaterialTheme.spacing
    val directDescription = stringResource(R.string.direct_title)
    SectionCard {
        SectionHeading(R.string.preferences_title, R.string.preferences_subtitle)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(SkyIcons.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.passengers_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.passengers_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onPassengersChanged(passengers - 1) }, enabled = passengers > 1,
                modifier = Modifier.testTag("passengers_minus")) {
                Icon(SkyIcons.Minus, contentDescription = stringResource(R.string.passengers_decrease))
            }
            Text(stringResource(R.string.passengers_number, passengers), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("passenger_count"))
            FilledTonalIconButton(onClick = { onPassengersChanged(passengers + 1) }, enabled = passengers < 9,
                modifier = Modifier.testTag("passengers_plus")) {
                Icon(SkyIcons.Plus, contentDescription = stringResource(R.string.passengers_increase))
            }
        }
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(stringResource(R.string.cabin_title), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                cabinLabels.forEachIndexed { index, label ->
                    FilterChip(
                        selected = cabin == index,
                        onClick = { onCabinChanged(index) },
                        label = { Text(label) },
                        leadingIcon = if (cabin == index) ({ Icon(SkyIcons.Check, contentDescription = null,
                            modifier = Modifier.size(18.dp)) }) else null,
                        modifier = Modifier.testTag("class_$index")
                    )
                }
            }
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(end = spacing.sm)) {
                Text(stringResource(R.string.direct_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.direct_subtitle), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = directOnly, onCheckedChange = onDirectChanged,
                modifier = Modifier.testTag("direct_only").semantics { contentDescription = directDescription },
                thumbContent = if (directOnly) ({ Icon(SkyIcons.Check, contentDescription = null,
                    modifier = Modifier.size(16.dp)) }) else null)
        }
    }
}

@Composable
private fun SearchSummary(
    origin: String,
    destination: String,
    passengerSummary: String,
    cabinLabel: String,
    directOnly: Boolean,
    onSearch: () -> Unit
) {
    val spacing = MaterialTheme.spacing
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.summary_route, origin, destination), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.summary_details, passengerSummary, cabinLabel),
                style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(if (directOnly) R.string.direct_title else R.string.connections_allowed),
                style = MaterialTheme.typography.bodySmall)
            Button(onClick = onSearch, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("search_flights")) {
                Icon(SkyIcons.Search, contentDescription = null)
                Spacer(Modifier.width(spacing.sm))
                Text(stringResource(R.string.search_flights), style = MaterialTheme.typography.titleMedium)
            }
            Text(stringResource(R.string.demo_notice), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = .8f))
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md), content = content)
    }
}

@Composable
private fun SectionHeading(@StringRes title: Int, @StringRes subtitle: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() })
        Text(stringResource(subtitle), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TravelDateDialog(
    initialDate: Long,
    minimumDate: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val selectableDates = remember(minimumDate) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= minimumDate
            override fun isSelectableYear(year: Int) = year >= Instant.ofEpochMilli(minimumDate)
                .atOffset(ZoneOffset.UTC).year
        }
    }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialDate,
        selectableDates = selectableDates)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.takeIf { it >= minimumDate }?.let(onConfirm) },
                enabled = state.selectedDateMillis?.let { it >= minimumDate } == true,
                modifier = Modifier.testTag("confirm_date")
            ) { Text(stringResource(R.string.confirm_date)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    ) { DatePicker(state = state) }
}

private const val MILLIS_PER_DAY = 86_400_000L

/** Small self-contained vectors avoid the much larger material-icons-extended dependency. */
private object SkyIcons {
    private fun icon(name: String, draw: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black), pathBuilder = draw)
        }.build()

    val Plane = icon("Plane") {
        moveTo(21f, 16f); lineTo(21f, 14f); lineTo(13f, 9f); lineTo(13f, 3.5f)
        curveTo(13f, 2.67f, 12.33f, 2f, 11.5f, 2f); curveTo(10.67f, 2f, 10f, 2.67f, 10f, 3.5f)
        lineTo(10f, 9f); lineTo(2f, 14f); lineTo(2f, 16f); lineTo(10f, 13.5f)
        lineTo(10f, 19f); lineTo(8f, 20.5f); lineTo(8f, 22f); lineTo(11.5f, 21f)
        lineTo(15f, 22f); lineTo(15f, 20.5f); lineTo(13f, 19f); lineTo(13f, 13.5f); close()
    }
    val Search = icon("Search") {
        moveTo(9.5f, 3f); curveTo(5.91f, 3f, 3f, 5.91f, 3f, 9.5f)
        curveTo(3f, 13.09f, 5.91f, 16f, 9.5f, 16f); curveTo(10.94f, 16f, 12.27f, 15.53f, 13.35f, 14.74f)
        lineTo(19.61f, 21f); lineTo(21f, 19.61f); lineTo(14.74f, 13.35f)
        curveTo(15.53f, 12.27f, 16f, 10.94f, 16f, 9.5f); curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f); close()
        moveTo(9.5f, 5f); curveTo(11.99f, 5f, 14f, 7.01f, 14f, 9.5f)
        curveTo(14f, 11.99f, 11.99f, 14f, 9.5f, 14f); curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
        curveTo(5f, 7.01f, 7.01f, 5f, 9.5f, 5f); close()
    }
    val Swap = icon("Swap") {
        moveTo(7f, 3f); lineTo(2f, 8f); lineTo(6f, 8f); lineTo(6f, 17f)
        lineTo(8f, 17f); lineTo(8f, 8f); lineTo(12f, 8f); close()
        moveTo(16f, 7f); lineTo(16f, 16f); lineTo(12f, 16f); lineTo(17f, 21f)
        lineTo(22f, 16f); lineTo(18f, 16f); lineTo(18f, 7f); close()
    }
    val Pin = icon("Pin") {
        moveTo(12f, 2f); curveTo(8.13f, 2f, 5f, 5.13f, 5f, 9f)
        curveTo(5f, 14.25f, 12f, 22f, 12f, 22f); curveTo(12f, 22f, 19f, 14.25f, 19f, 9f)
        curveTo(19f, 5.13f, 15.87f, 2f, 12f, 2f); close()
        moveTo(12f, 6.5f); curveTo(13.38f, 6.5f, 14.5f, 7.62f, 14.5f, 9f)
        curveTo(14.5f, 10.38f, 13.38f, 11.5f, 12f, 11.5f)
        curveTo(10.62f, 11.5f, 9.5f, 10.38f, 9.5f, 9f); curveTo(9.5f, 7.62f, 10.62f, 6.5f, 12f, 6.5f); close()
    }
    val Calendar = icon("Calendar") {
        moveTo(19f, 4f); lineTo(18f, 4f); lineTo(18f, 2f); lineTo(16f, 2f); lineTo(16f, 4f)
        lineTo(8f, 4f); lineTo(8f, 2f); lineTo(6f, 2f); lineTo(6f, 4f); lineTo(5f, 4f)
        curveTo(3.9f, 4f, 3f, 4.9f, 3f, 6f); lineTo(3f, 20f); curveTo(3f, 21.1f, 3.9f, 22f, 5f, 22f)
        lineTo(19f, 22f); curveTo(20.1f, 22f, 21f, 21.1f, 21f, 20f); lineTo(21f, 6f)
        curveTo(21f, 4.9f, 20.1f, 4f, 19f, 4f); close()
        moveTo(19f, 20f); lineTo(5f, 20f); lineTo(5f, 9f); lineTo(19f, 9f); close()
    }
    val Person = icon("Person") {
        moveTo(12f, 2f); curveTo(9.79f, 2f, 8f, 3.79f, 8f, 6f)
        curveTo(8f, 8.21f, 9.79f, 10f, 12f, 10f); curveTo(14.21f, 10f, 16f, 8.21f, 16f, 6f)
        curveTo(16f, 3.79f, 14.21f, 2f, 12f, 2f); close()
        moveTo(12f, 12f); curveTo(7.58f, 12f, 4f, 14.24f, 4f, 17f)
        lineTo(4f, 20f); lineTo(20f, 20f); lineTo(20f, 17f)
        curveTo(20f, 14.24f, 16.42f, 12f, 12f, 12f); close()
    }
    val Check = icon("Check") {
        moveTo(9f, 16.17f); lineTo(4.83f, 12f); lineTo(3.41f, 13.41f)
        lineTo(9f, 19f); lineTo(21f, 7f); lineTo(19.59f, 5.59f); close()
    }
    val Plus = icon("Plus") {
        moveTo(19f, 11f); lineTo(13f, 11f); lineTo(13f, 5f); lineTo(11f, 5f)
        lineTo(11f, 11f); lineTo(5f, 11f); lineTo(5f, 13f); lineTo(11f, 13f)
        lineTo(11f, 19f); lineTo(13f, 19f); lineTo(13f, 13f); lineTo(19f, 13f); close()
    }
    val Minus = icon("Minus") {
        moveTo(5f, 11f); lineTo(19f, 11f); lineTo(19f, 13f); lineTo(5f, 13f); close()
    }
    val Heart = icon("Heart") {
        moveTo(12f, 21f); lineTo(3.5f, 12.5f); curveTo(-2f, 7f, 5f, -1f, 12f, 5.5f)
        curveTo(19f, -1f, 26f, 7f, 20.5f, 12.5f); close()
    }
    val Ticket = icon("Ticket") {
        moveTo(20f, 4f); lineTo(4f, 4f); curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f)
        lineTo(2f, 9f); curveTo(3.66f, 9f, 5f, 10.34f, 5f, 12f)
        curveTo(5f, 13.66f, 3.66f, 15f, 2f, 15f); lineTo(2f, 18f)
        curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f); lineTo(20f, 20f)
        curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f); lineTo(22f, 15f)
        curveTo(20.34f, 15f, 19f, 13.66f, 19f, 12f); curveTo(19f, 10.34f, 20.34f, 9f, 22f, 9f)
        lineTo(22f, 6f); curveTo(22f, 4.9f, 21.1f, 4f, 20f, 4f); close()
    }
}

@Preview(name = "Russian · light", locale = "ru", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "English · light", locale = "en", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Russian · dark", locale = "ru", uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Tablet · English", locale = "en", widthDp = 1100, heightDp = 900, showBackground = true)
@Composable
private fun FlightSearchPreview() {
    SkyWayTheme(dynamicColor = false) {
        CompositionLocalProvider(LocalPlaceholderNotifier provides PlaceholderNotifier {}) {
            Surface { FlightSearchScreen() }
        }
    }
}
