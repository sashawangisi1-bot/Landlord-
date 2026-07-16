package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                JeblandlordApp()
            }
        }
    }
}

// Data Classes for Property Management
data class Landlord(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val commissionPercent: Int = 10,
    val status: String = "Active", // "Active", "Pending Setup", "Suspended"
    val onboardingDate: String = "15 Jul 2026"
)

data class ClientRequest(
    val id: String,
    val tenantName: String,
    val unit: String,
    val requestType: String, // "Repairs", "Painting", "Comment"
    val description: String,
    val requestDate: String = "15 Jul 2026",
    val status: String = "Pending", // "Pending", "Scheduled", "Completed"
    val resolutionNotes: String = ""
)

data class House(
    val id: String,
    val property: String,
    val unit: String,
    val type: String,
    val rent: Int,
    val status: String, // Occupied, Vacant, Arrears
    val tenant: String,
    val maintenanceStatus: String = "Operational", // "Operational", "Under Maintenance", "Repairs Scheduled", "Inspection Required"
    val lastMaintenanceDate: String = "—",
    val meterNo: String = "—",
    val notes: String = "",
    val landlordId: String = "L-01"
)

data class Tenant(
    val name: String,
    val unit: String,
    val phone: String,
    val rent: Int,
    val status: String, // Compliant, 5–15 days, >15 days
    val lastPayment: String
)

data class RentBilling(
    val month: String,
    val unit: String,
    val tenant: String,
    val billed: Int,
    val paid: Int,
    val balance: Int
)

data class WaterBilling(
    val unit: String,
    val prevReading: Int,
    val currReading: Int,
    val units: Int,
    val rate: Int,
    val amount: Int,
    val status: String // Paid, Unpaid
)

data class PaymentReceived(
    val date: String,
    val ref: String,
    val tenant: String,
    val method: String,
    val amount: Int,
    val allocated: Boolean
)

data class Allocation(
    val ref: String,
    val amount: Int,
    val receivedOn: String,
    val suggestedUnit: String,
    val note: String
)

data class SentMessage(
    val id: String,
    val date: String,
    val recipients: String,
    val messageText: String,
    val status: String,
    val category: String
)

data class MeterReplacement(
    val id: String,
    val unit: String,
    val requestDate: String,
    val reason: String,
    val cost: Int,
    val status: String
)

data class Expense(
    val id: String,
    val date: String,
    val category: String,
    val description: String,
    val amount: Int
)

data class DepositEntry(
    val tenantName: String,
    val unit: String,
    val amountDeposited: Int,
    val dateDeposited: String,
    val amountRefunded: Int = 0,
    val amountClaimed: Int = 0,
    val status: String, // "Held in Trust", "Partially Refunded", "Fully Refunded", "Claimed in Full"
    val notes: String = ""
)

data class DepositTransaction(
    val id: String,
    val tenantName: String,
    val date: String,
    val type: String, // "Deposit Received", "Refund Issued", "Claim Deducted"
    val amount: Int,
    val reason: String
)

// Main Applet Container
@Composable
fun JeblandlordApp() {
    var isStarted by remember { mutableStateOf(false) }
    var activeView by remember { mutableStateOf("dashboard") }
    var searchText by remember { mutableStateOf("") }
    var isSidebarOpen by remember { mutableStateOf(false) }

    // Role & simulation states for SaaS reseller
    var simulatedRole by remember { mutableStateOf("Admin / Reseller") } // "Admin / Reseller", "Landlord View", "Tenant Portal"
    var selectedLandlordId by remember { mutableStateOf("L-01") }
    var globalSelectedTenantName by remember { mutableStateOf("Wanjiru M.") }

    // Dialog state for adding entities
    var showAddTenantDialog by remember { mutableStateOf(false) }
    var showAddHouseDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showManageHouseDialog by remember { mutableStateOf<House?>(null) }
    var showAddLandlordDialog by remember { mutableStateOf(false) }

    // Reactive State Lists
    val landlords = remember {
        mutableStateListOf(
            Landlord("L-01", "Hon. Jeblord", "jeblord@gmail.com", "0722 999 888", 12, "Active", "12 May 2026"),
            Landlord("L-02", "Mama Greenview", "greenview@flats.co.ke", "0733 555 444", 10, "Active", "18 Jun 2026"),
            Landlord("L-03", "Rongai Heights Ltd", "info@rongaiheights.com", "0711 666 555", 15, "Active", "22 Jun 2026")
        )
    }

    val clientRequests = remember {
        mutableStateListOf(
            ClientRequest("REQ-101", "Wanjiru M.", "Amani Court – A1", "Repairs", "The hot water shower system is leaking from the upper joint.", "10 Jul 2026", "Pending"),
            ClientRequest("REQ-102", "Otieno K.", "Amani Court – A2", "Painting", "Requesting a repaint of the balcony wall due to sun peeling.", "12 Jul 2026", "Scheduled", "Scheduled for 20th July"),
            ClientRequest("REQ-103", "Mwangi P.", "Greenview – B2", "Comment", "Water flow in the kitchen has been excellent, thank you for the prompt meter fix last week.", "14 Jul 2026", "Completed", "Noted by management")
        )
    }

    val houses = remember {
        mutableStateListOf(
            House("H-01", "Amani Court", "A1", "Bedsitter", 6000, "Occupied", "Wanjiru M.", "Operational", "12 May 2026", "MTR-A1", "Sink drainage checked and cleared.", "L-01"),
            House("H-02", "Amani Court", "A2", "1BR", 9500, "Occupied", "Otieno K.", "Operational", "04 Jun 2026", "MTR-A2", "Minor electrical switch replaced.", "L-01"),
            House("H-03", "Amani Court", "A3", "1BR", 9500, "Vacant", "—", "Inspection Required", "01 Jul 2026", "MTR-A3", "Vacated. Requires light paint touch-ups.", "L-01"),
            House("H-04", "Greenview Flats", "B1", "2BR", 15000, "Occupied", "Chebet A.", "Operational", "18 Jun 2026", "MTR-B1", "Perfect status. Main door lock lubricated.", "L-02"),
            House("H-05", "Greenview Flats", "B2", "2BR", 15000, "Arrears", "Mwangi P.", "Repairs Scheduled", "—", "MTR-B2", "Reported broken window latch in kitchen.", "L-02"),
            House("H-06", "Greenview Flats", "B3", "Bedsitter", 6500, "Vacant", "—", "Under Maintenance", "10 Jul 2026", "MTR-B3", "Major bathroom tiling overhaul ongoing.", "L-02"),
            House("H-07", "Rongai Heights", "C1", "1BR", 8000, "Occupied", "Kiptoo J.", "Operational", "22 Jun 2026", "MTR-C1", "No known issues. Water valve tested ok.", "L-03"),
            House("H-08", "Rongai Heights", "C2", "Studio", 5500, "Arrears", "Nduta L.", "Operational", "08 Apr 2026", "MTR-C2", "Standard inspection passed.", "L-03")
        )
    }

    val tenants = remember {
        mutableStateListOf(
            Tenant("Wanjiru M.", "Amani Court – A1", "0722 111 222", 6000, "Compliant", "05 Jul 2026"),
            Tenant("Otieno K.", "Amani Court – A2", "0733 222 333", 9500, "Compliant", "03 Jul 2026"),
            Tenant("Chebet A.", "Greenview – B1", "0711 333 444", 15000, "Compliant", "01 Jul 2026"),
            Tenant("Mwangi P.", "Greenview – B2", "0744 444 555", 15000, "5–15 days", "22 Jun 2026"),
            Tenant("Kiptoo J.", "Rongai Heights – C1", "0755 555 666", 8000, "Compliant", "04 Jul 2026"),
            Tenant("Nduta L.", "Rongai Heights – C2", "0799 666 777", 5500, ">15 days", "12 Jun 2026")
        )
    }

    val rentBillings = remember {
        mutableStateListOf(
            RentBilling("Jul 2026", "Amani Court – A1", "Wanjiru M.", 6000, 6000, 0),
            RentBilling("Jul 2026", "Amani Court – A2", "Otieno K.", 9500, 9500, 0),
            RentBilling("Jul 2026", "Greenview – B1", "Chebet A.", 15000, 15000, 0),
            RentBilling("Jul 2026", "Greenview – B2", "Mwangi P.", 15000, 6000, 9000),
            RentBilling("Jul 2026", "Rongai Heights – C1", "Kiptoo J.", 8000, 8000, 0),
            RentBilling("Jul 2026", "Rongai Heights – C2", "Nduta L.", 5500, 0, 5500)
        )
    }

    val waterBillings = remember {
        mutableStateListOf(
            WaterBilling("Amani Court – A1", 120, 128, 8, 120, 960, "Paid"),
            WaterBilling("Amani Court – A2", 340, 351, 11, 120, 1320, "Paid"),
            WaterBilling("Greenview – B1", 210, 224, 14, 120, 1680, "Paid"),
            WaterBilling("Greenview – B2", 88, 95, 7, 120, 840, "Unpaid"),
            WaterBilling("Rongai Heights – C1", 402, 414, 12, 120, 1440, "Paid"),
            WaterBilling("Rongai Heights – C2", 176, 181, 5, 120, 600, "Unpaid")
        )
    }

    val paymentsReceived = remember {
        mutableStateListOf(
            PaymentReceived("10 Jul 2026", "SKA7X2LQ", "Wanjiru M.", "M-Pesa", 6000, true),
            PaymentReceived("09 Jul 2026", "SKA6M9PZ", "Otieno K.", "M-Pesa", 9500, true),
            PaymentReceived("08 Jul 2026", "SKA5R1TN", "Chebet A.", "Bank", 15000, true),
            PaymentReceived("06 Jul 2026", "SKA4Q8VD", "Mwangi P.", "M-Pesa", 6000, true),
            PaymentReceived("04 Jul 2026", "SKA3H4XE", "Kiptoo J.", "M-Pesa", 8000, true),
            PaymentReceived("02 Jul 2026", "SKA2J7YB", "Unallocated", "M-Pesa", 3200, false)
        )
    }

    val allocations = remember {
        mutableStateListOf(
            Allocation("SKA2J7YB", 3200, "02 Jul 2026", "Rongai Heights – C2", "Amount below rent – partial payment"),
            Allocation("SKA9F0KM", 1500, "29 Jun 2026", "—", "No matching phone number on file")
        )
    }

    // Additional interactive state lists & config parameters for advanced landlord utility tools
    val sentMessages = remember {
        mutableStateListOf(
            SentMessage("MSG-101", "12 Jul 2026", "All Tenants", "Dear Tenants, water readings for the June-July cycle have been completed. Please check your dashboard.", "Delivered", "Announcement"),
            SentMessage("MSG-102", "05 Jul 2026", "Mwangi P., Nduta L.", "Dear tenant, this is a friendly reminder that your July lease installment remains pending.", "Delivered", "Rent Reminder"),
            SentMessage("MSG-103", "20 Jun 2026", "Amani Court – A1", "Urgent: Water meter replacement scheduled for A1 on 22 June 10 AM.", "Delivered", "Utility Notice")
        )
    }

    val meterReplacements = remember {
        mutableStateListOf(
            MeterReplacement("MTR-501", "Amani Court – A1", "10 Jul 2026", "Faulty reading (Spinning too fast)", 4500, "Completed"),
            MeterReplacement("MTR-502", "Rongai Heights – C2", "14 Jul 2026", "Glass cover cracked and leaking", 4500, "Pending Approval")
        )
    }

    val expenses = remember {
        mutableStateListOf(
            Expense("EXP-901", "04 Jul 2026", "Plumbing", "Unblocking sewers in Rongai Heights", 6500),
            Expense("EXP-902", "01 Jul 2026", "Cleaning", "Fumigation & common area sanitation", 8000),
            Expense("EXP-903", "28 Jun 2026", "Nakuru Water", "Main bulk utility water deposit bill", 18500)
        )
    }

    val deposits = remember {
        mutableStateListOf(
            DepositEntry("Wanjiru M.", "Amani Court – A1", 6000, "01 Jul 2026", 0, 0, "Held in Trust", "First-month standard deposit paid in full."),
            DepositEntry("Otieno K.", "Amani Court – A2", 9500, "01 Jul 2026", 0, 0, "Held in Trust", "Lease agreement signed. Paid on intake."),
            DepositEntry("Chebet A.", "Greenview – B1", 15000, "28 Jun 2026", 0, 0, "Held in Trust", "Premium double bedroom deposit."),
            DepositEntry("Mwangi P.", "Greenview – B2", 15000, "15 Jun 2026", 0, 0, "Held in Trust", "Subject to late-payment observation."),
            DepositEntry("Kiptoo J.", "Rongai Heights – C1", 8000, "01 Jun 2026", 0, 0, "Held in Trust", "Held safely under escrow."),
            DepositEntry("Nduta L.", "Rongai Heights – C2", 5500, "10 Jun 2026", 0, 0, "Held in Trust", "Standard studio deposit."),
            DepositEntry("Kamau S.", "Amani Court – A3", 9500, "01 Jan 2026", 9500, 0, "Fully Refunded", "Lease terminated smoothly. Handed key back."),
            DepositEntry("Moraa J.", "Greenview – B3", 6500, "10 Feb 2026", 4000, 2500, "Partially Refunded", "Deducted 2,500 KSh for wall repainting.")
        )
    }

    val depositTransactions = remember {
        mutableStateListOf(
            DepositTransaction("TXD-101", "Wanjiru M.", "01 Jul 2026", "Deposit Received", 6000, "Initial intake security deposit"),
            DepositTransaction("TXD-102", "Otieno K.", "01 Jul 2026", "Deposit Received", 9500, "Initial intake security deposit"),
            DepositTransaction("TXD-103", "Chebet A.", "28 Jun 2026", "Deposit Received", 15000, "Initial intake security deposit"),
            DepositTransaction("TXD-104", "Kamau S.", "30 Jun 2026", "Refund Issued", 9500, "Move-out full refund processed"),
            DepositTransaction("TXD-105", "Moraa J.", "10 Jul 2026", "Claim Deducted", 2500, "Damaged bedroom wall repainting fee"),
            DepositTransaction("TXD-106", "Moraa J.", "10 Jul 2026", "Refund Issued", 4000, "Remaining security deposit balance release")
        )
    }

    var defaultWaterRate by remember { mutableStateOf(120) }
    var latePaymentPenaltyPercent by remember { mutableStateOf(5) }
    var billingDayOfMonth by remember { mutableStateOf(5) }

    val trendData = listOf(118000, 124000, 109000, 131000, 96000, 152000, 61000)
    val trendMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul")

    // Calculations based on lists
    val totalRentExpected = rentBillings.sumOf { it.billed }
    val totalCollected = rentBillings.sumOf { it.paid }
    val totalArrears = rentBillings.sumOf { it.balance }
    val totalWaterBilled = waterBillings.sumOf { it.amount }
    val occupiedUnits = houses.count { it.status == "Occupied" || it.status == "Arrears" }
    val vacantUnits = houses.count { it.status == "Vacant" }
    val unallocatedPaymentsCount = paymentsReceived.count { !it.allocated }
    val rentCollectedPercent = if (totalRentExpected > 0) ((totalCollected.toFloat() / totalRentExpected.toFloat()) * 100).toInt() else 0

    // Dynamic Filtered Lists/Metrics for Role Simulation
    val displayHouses = if (simulatedRole == "Landlord View") {
        houses.filter { it.landlordId == selectedLandlordId }
    } else {
        houses
    }

    val displayTenants = if (simulatedRole == "Landlord View") {
        tenants.filter { t -> displayHouses.any { h -> t.unit.contains(h.unit) } }
    } else {
        tenants
    }

    val displayRentBillings = if (simulatedRole == "Landlord View") {
        rentBillings.filter { rb -> displayHouses.any { h -> rb.unit.contains(h.unit) } }.toMutableStateList()
    } else {
        rentBillings
    }

    val displayWaterBillings = if (simulatedRole == "Landlord View") {
        waterBillings.filter { wb -> displayHouses.any { h -> wb.unit.contains(h.unit) } }.toMutableStateList()
    } else {
        waterBillings
    }

    val displayTotalRentExpected = displayRentBillings.sumOf { it.billed }
    val displayTotalCollected = displayRentBillings.sumOf { it.paid }
    val displayTotalArrears = displayRentBillings.sumOf { it.balance }
    val displayTotalWaterBilled = displayWaterBillings.sumOf { it.amount }
    val displayOccupiedUnits = displayHouses.count { it.status == "Occupied" || it.status == "Arrears" }
    val displayVacantUnits = displayHouses.count { it.status == "Vacant" }
    val displayRentCollectedPercent = if (displayTotalRentExpected > 0) ((displayTotalCollected.toFloat() / displayTotalRentExpected.toFloat()) * 100).toInt() else 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize().testTag("main_scaffold")
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Crossfade(targetState = isStarted, label = "OnboardingTransition") { started ->
                if (!started) {
                    LandingPage(onGetStarted = { isStarted = true })
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isWideScreen = maxWidth >= 720.dp
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Desktop Navigation Drawer (Left sidebar) for wider screens
                            if (isWideScreen) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(260.dp)
                                        .background(HighDensitySurfaceVariant)
                                        .border(width = 1.dp, color = HighDensityOutline, shape = RoundedCornerShape(0.dp))
                                        .padding(vertical = 12.dp)
                                        .then(Modifier.verticalScroll(rememberScrollState()))
                                ) {
                                    SidebarMenu(
                                        activeView = activeView,
                                        onViewSelected = {
                                            activeView = it
                                            searchText = ""
                                        }
                                    )
                                }
                            }

                            // Main Area
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.background)
                            ) {
                                // Top Header Bar with Search & User initials
                                TopHeader(
                                    title = getPageTitle(activeView),
                                    searchText = searchText,
                                    onSearchChange = { searchText = it },
                                    onToggleMenu = { isSidebarOpen = !isSidebarOpen },
                                    onBackToOnboarding = { isStarted = false },
                                    showMenuButton = !isWideScreen
                                )

                                // SaaS Platform Controller Banner
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = HighDensityPrimaryContainer),
                                    border = BorderStroke(1.dp, HighDensityPrimary.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = null,
                                                    tint = HighDensityPrimary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = "RESELLER PLATFORM CONTROLLER",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = HighDensityPrimary,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(HighDensityPrimary, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = simulatedRole.uppercase(),
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Role:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                                                listOf("Admin / Reseller", "Landlord View", "Tenant Portal").forEach { r ->
                                                    val isSel = simulatedRole == r
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                if (isSel) HighDensityPrimary else Color.White,
                                                                RoundedCornerShape(12.dp)
                                                            )
                                                            .border(
                                                                1.dp,
                                                                if (isSel) HighDensityPrimary else HighDensityOutline,
                                                                RoundedCornerShape(12.dp)
                                                            )
                                                            .clickable {
                                                                simulatedRole = r
                                                                if (r == "Tenant Portal") {
                                                                    activeView = "tenant_portal"
                                                                } else if (r == "Landlord View") {
                                                                    activeView = "dashboard"
                                                                } else {
                                                                    activeView = "saas_manager"
                                                                }
                                                            }
                                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = r,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSel) Color.White else HighDensityPrimary
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            if (simulatedRole == "Landlord View") {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text("Landlord:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                                                    landlords.forEach { landlord ->
                                                        val isSel = selectedLandlordId == landlord.id
                                                        Box(
                                                            modifier = Modifier
                                                                .background(
                                                                    if (isSel) HighDensitySecondary else Color.White,
                                                                    RoundedCornerShape(4.dp)
                                                                )
                                                                .border(
                                                                    1.dp,
                                                                    if (isSel) HighDensitySecondary else HighDensityOutline,
                                                                    RoundedCornerShape(4.dp)
                                                                )
                                                                .clickable { selectedLandlordId = landlord.id }
                                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                                        ) {
                                                            Text(
                                                                text = landlord.name.substringBefore(" "),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isSel) Color.White else HighDensityText
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    RenderActiveView(
                                        view = activeView,
                                        searchText = searchText,
                                        houses = displayHouses,
                                        tenants = displayTenants,
                                        rentBillings = displayRentBillings,
                                        waterBillings = displayWaterBillings,
                                        paymentsReceived = paymentsReceived,
                                        allocations = allocations,
                                        totalRentExpected = displayTotalRentExpected,
                                        totalCollected = displayTotalCollected,
                                        totalArrears = displayTotalArrears,
                                        totalWaterBilled = displayTotalWaterBilled,
                                        occupiedUnits = displayOccupiedUnits,
                                        vacantUnits = displayVacantUnits,
                                        unallocatedPaymentsCount = unallocatedPaymentsCount,
                                        rentCollectedPercent = displayRentCollectedPercent,
                                        trendData = trendData,
                                        trendMonths = trendMonths,
                                        sentMessages = sentMessages,
                                        meterReplacements = meterReplacements,
                                        expenses = expenses,
                                        defaultWaterRate = defaultWaterRate,
                                        onDefaultWaterRateChange = { defaultWaterRate = it },
                                        latePaymentPenaltyPercent = latePaymentPenaltyPercent,
                                        onLatePaymentPenaltyPercentChange = { latePaymentPenaltyPercent = it },
                                        billingDayOfMonth = billingDayOfMonth,
                                        onBillingDayOfMonthChange = { billingDayOfMonth = it },
                                        onNavigate = { activeView = it },
                                        onAddTenantClick = { showAddTenantDialog = true },
                                        onAddHouseClick = { showAddHouseDialog = true },
                                        onAddPaymentClick = { showAddPaymentDialog = true },
                                        onManageHouseClick = { showManageHouseDialog = it },
                                        deposits = deposits,
                                        depositTransactions = depositTransactions,
                                        landlords = landlords,
                                        clientRequests = clientRequests,
                                        onAddLandlordClick = { showAddLandlordDialog = true },
                                        onUpdateClientRequestStatus = { reqId, newStatus ->
                                            val rIdx = clientRequests.indexOfFirst { it.id == reqId }
                                            if (rIdx != -1) {
                                                val r = clientRequests[rIdx]
                                                clientRequests[rIdx] = r.copy(
                                                    status = newStatus,
                                                    resolutionNotes = if (newStatus == "Completed") "Resolved by owner on 15 Jul" else "Inspection scheduled"
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Sliding drawer overlay for smaller widths / custom menu trigger
                        if (isSidebarOpen && !isWideScreen) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable { isSidebarOpen = false }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(265.dp)
                                        .background(HighDensitySurfaceVariant)
                                        .align(Alignment.CenterStart)
                                        .clickable(enabled = false) {}
                                        .padding(vertical = 12.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "JEBLANDLORD",
                                                fontWeight = FontWeight.Bold,
                                                color = HighDensityPrimary,
                                                fontSize = 16.sp
                                            )
                                            IconButton(onClick = { isSidebarOpen = false }) {
                                                Icon(Icons.Default.Close, contentDescription = "Close Menu")
                                            }
                                        }
                                        HorizontalDivider(color = HighDensityOutline)
                                        Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                            SidebarMenu(
                                                activeView = activeView,
                                                onViewSelected = {
                                                    activeView = it
                                                    searchText = ""
                                                    isSidebarOpen = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dialogs for Adding records
            if (showAddTenantDialog) {
                AddTenantDialog(
                    onDismiss = { showAddTenantDialog = false },
                    onConfirm = { name, unit, phone, rentVal ->
                        tenants.add(0, Tenant(name, unit, phone, rentVal, "Compliant", "Today"))
                        rentBillings.add(0, RentBilling("Jul 2026", unit, name, rentVal, 0, rentVal))
                        showAddTenantDialog = false
                    },
                    housesList = houses.map { "${it.property} – ${it.unit}" }
                )
            }

            if (showAddHouseDialog) {
                AddHouseDialog(
                    onDismiss = { showAddHouseDialog = false },
                    onConfirm = { property, unit, type, rentVal, landlordId ->
                        houses.add(0, House(
                            id = "H-${houses.size + 1}",
                            property = property,
                            unit = unit,
                            type = type,
                            rent = rentVal,
                            status = "Vacant",
                            tenant = "—",
                            meterNo = "MTR-$unit",
                            landlordId = landlordId
                        ))
                        showAddHouseDialog = false
                    },
                    landlords = landlords,
                    initialLandlordId = if (simulatedRole == "Landlord View") selectedLandlordId else "L-01"
                )
            }

            if (showAddLandlordDialog) {
                AddLandlordDialog(
                    onDismiss = { showAddLandlordDialog = false },
                    onConfirm = { name, email, phone, commissionPercent ->
                        val newId = "L-${String.format("%02d", landlords.size + 1)}"
                        landlords.add(0, Landlord(
                            id = newId,
                            name = name,
                            email = email,
                            phone = phone,
                            commissionPercent = commissionPercent,
                            status = "Active",
                            onboardingDate = "15 Jul 2026"
                        ))
                        showAddLandlordDialog = false
                    }
                )
            }

            if (showAddPaymentDialog) {
                AddPaymentDialog(
                    onDismiss = { showAddPaymentDialog = false },
                    onConfirm = { date, ref, tenant, method, amount, allocated ->
                        paymentsReceived.add(0, PaymentReceived(date, ref, tenant, method, amount, allocated))
                        if (allocated) {
                            // Find tenant in rentBillings and update paid status
                            val index = rentBillings.indexOfFirst { it.tenant.equals(tenant, ignoreCase = true) }
                            if (index != -1) {
                                val b = rentBillings[index]
                                rentBillings[index] = b.copy(
                                    paid = b.paid + amount,
                                    balance = maxOf(0, b.billed - (b.paid + amount))
                                )
                            }
                        } else {
                            allocations.add(0, Allocation(ref, amount, date, "Auto Suggest", "Awaiting allocation check"))
                        }
                        showAddPaymentDialog = false
                    },
                    tenantsList = tenants.map { it.name }
                )
            }

            if (showManageHouseDialog != null) {
                ManageHouseDialog(
                    house = showManageHouseDialog!!,
                    tenantsList = tenants.map { it.name },
                    onDismiss = { showManageHouseDialog = null },
                    onConfirm = { updatedHouse ->
                        val index = houses.indexOfFirst { it.id == updatedHouse.id }
                        if (index != -1) {
                            houses[index] = updatedHouse
                            
                            // If occupancy status changed, also update/synchronize tenant lists or rent records
                            if (updatedHouse.status == "Vacant") {
                                val tenantIdx = tenants.indexOfFirst { it.unit.contains(updatedHouse.unit) }
                                if (tenantIdx != -1) {
                                    tenants.removeAt(tenantIdx)
                                }
                                val billingIdx = rentBillings.indexOfFirst { it.unit.contains(updatedHouse.unit) }
                                if (billingIdx != -1) {
                                    rentBillings.removeAt(billingIdx)
                                }
                            } else if (updatedHouse.tenant != "—" && updatedHouse.tenant.isNotEmpty()) {
                                // Add or update tenant record
                                val tenantIdx = tenants.indexOfFirst { it.name.equals(updatedHouse.tenant, ignoreCase = true) }
                                if (tenantIdx == -1) {
                                    tenants.add(0, Tenant(updatedHouse.tenant, "${updatedHouse.property} – ${updatedHouse.unit}", "0700 000 000", updatedHouse.rent, "Compliant", "Today"))
                                }
                                val billingIdx = rentBillings.indexOfFirst { it.tenant.equals(updatedHouse.tenant, ignoreCase = true) }
                                if (billingIdx == -1) {
                                    rentBillings.add(0, RentBilling("Jul 2026", "${updatedHouse.property} – ${updatedHouse.unit}", updatedHouse.tenant, updatedHouse.rent, 0, updatedHouse.rent))
                                }
                            }
                        }
                        showManageHouseDialog = null
                    },
                    onDelete = { houseId ->
                        houses.removeAll { it.id == houseId }
                        showManageHouseDialog = null
                    }
                )
            }
        }
    }
}

// 1. Landing Onboarding Page
@Composable
fun LandingPage(onGetStarted: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(HighDensityBg)
    ) {
        val isWideScreen = maxWidth >= 720.dp
        if (isWideScreen) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.spacedBy(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Branding and premium Hero Card
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.jeblord_logo_1784052742263),
                            contentDescription = "Jeblord Agency Group Logo",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(2.dp, HighDensitySecondary, RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = "JEBLANDLORD",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityPrimary,
                                fontFamily = FontFamily.Serif
                            )
                            Text(
                                text = "Connecting you to the World",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = HighDensitySecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(HighDensityPrimaryContainer)
                            .padding(28.dp)
                    ) {
                        Column {
                            Text(
                                text = "PROPERTIES CONTROL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityPrimary,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Integrated Landlord Management",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityPrimary,
                                lineHeight = 34.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Consolidated system tracking monthly rent billings, water cycle charges, unallocated payment tracking, and analytics across all Nakuru units.",
                                fontSize = 15.sp,
                                color = HighDensityPrimary.copy(alpha = 0.85f),
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                // Right Column: Onboarding features list card & Launch Button
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(0.9f)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, HighDensityOutline, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(28.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "Platform Features",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FeatureOnboardingItem(
                                title = "Smart Rent Cycle Billings",
                                desc = "Consolidate active tenants with automated billing ledger tracking.",
                                icon = Icons.Default.Receipt
                            )
                            FeatureOnboardingItem(
                                title = "Water Meter Readings",
                                desc = "Log start/end readings, water units consumption, and specific municipal rates.",
                                icon = Icons.Default.WaterDrop
                            )
                            FeatureOnboardingItem(
                                title = "Finance Allocations Hub",
                                desc = "Track unallocated incoming bank or M-Pesa statements with auto-matching.",
                                icon = Icons.Default.AccountBalanceWallet
                            )
                        }

                        Button(
                            onClick = onGetStarted,
                            colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("get_started_button")
                        ) {
                            Text(
                                text = "Launch Landlord Dashboard",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo representation with the generated Jeblord Agency Group logo image
                Image(
                    painter = painterResource(id = R.drawable.jeblord_logo_1784052742263),
                    contentDescription = "Jeblord Agency Group Logo",
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(2.dp, HighDensitySecondary, RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Branding
                Text(
                    text = "JEBLANDLORD",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityPrimary,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "Connecting you to the World",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = HighDensitySecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Hero Card (High Density Styling)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(HighDensityPrimaryContainer)
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "PROPERTIES CONTROL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityPrimary,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Integrated Landlord Management",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityPrimary,
                            lineHeight = 32.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Consolidated system tracking monthly rent billings, water cycle charges, unallocated payment tracking, and analytics across all Nakuru units.",
                            fontSize = 14.sp,
                            color = HighDensityPrimary.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Onboarding Feature list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FeatureOnboardingItem(
                        title = "Smart Rent Cycle Billings",
                        desc = "Consolidate active tenants with automated billing ledger tracking.",
                        icon = Icons.Default.Receipt
                    )
                    FeatureOnboardingItem(
                        title = "Water Meter Readings",
                        desc = "Log start/end readings, water units consumption, and specific municipal rates.",
                        icon = Icons.Default.WaterDrop
                    )
                    FeatureOnboardingItem(
                        title = "Finance Allocations Hub",
                        desc = "Track unallocated incoming bank or M-Pesa statements with auto-matching.",
                        icon = Icons.Default.AccountBalanceWallet
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // CTA Button
                Button(
                    onClick = onGetStarted,
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("get_started_button")
                ) {
                    Text(
                        text = "Launch Landlord Dashboard",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureOnboardingItem(title: String, desc: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HighDensitySurfaceVariant)
            .border(1.dp, HighDensityOutline, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(HighDensitySecondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = HighDensityPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
            Text(text = desc, fontSize = 13.sp, color = HighDensitySecondaryText, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// 2. Top Header Bar
@Composable
fun TopHeader(
    title: String,
    searchText: String,
    onSearchChange: (String) -> Unit,
    onToggleMenu: () -> Unit,
    onBackToOnboarding: () -> Unit,
    showMenuButton: Boolean = true
) {
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showMenuButton) {
                    IconButton(onClick = onToggleMenu) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = HighDensityPrimary)
                    }
                }
                IconButton(onClick = onBackToOnboarding) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Info", tint = HighDensitySecondary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityText,
                    fontFamily = FontFamily.Serif
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Interactive Search Textbox
                OutlinedTextField(
                    value = searchText,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search...", fontSize = 12.sp, color = HighDensitySecondaryText) },
                    modifier = Modifier
                        .width(160.dp)
                        .height(48.dp)
                        .testTag("search_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighDensityPrimary,
                        unfocusedBorderColor = HighDensityOutline,
                        focusedContainerColor = HighDensityBg,
                        unfocusedContainerColor = HighDensityBg
                    ),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = HighDensitySecondaryText) }
                )

                // User Initials Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(HighDensityPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("JL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

// 3. Sidebar Menu Layout
@Composable
fun SidebarMenu(
    activeView: String,
    onViewSelected: (String) -> Unit
) {
    val sections = listOf(
        "Overview" to listOf(
            Triple("saas_manager", "SaaS Reseller Hub", Icons.Default.Business),
            Triple("dashboard", "Dashboard", Icons.Default.Dashboard),
            Triple("tenant_portal", "Tenant Portal", Icons.Default.Person),
            Triple("messages", "Messages", Icons.Default.Message),
            Triple("reports", "Reports", Icons.Default.Assessment),
            Triple("sent", "Sent Messages", Icons.Default.Send)
        ),
        "Property Management" to listOf(
            Triple("tenants", "Tenants", Icons.Default.People),
            Triple("houses", "Houses / Units", Icons.Default.Home),
            Triple("billing_table", "Billing Table", Icons.Default.ListAlt),
            Triple("rent", "Rent Billings", Icons.Default.Receipt),
            Triple("water", "Water Billings", Icons.Default.WaterDrop),
            Triple("meters", "Meter Replacement", Icons.Default.Build)
        ),
        "Finance" to listOf(
            Triple("payment_dashboard", "Payment Dashboard", Icons.Default.Assessment),
            Triple("config", "System Config", Icons.Default.Settings),
            Triple("deposits", "Tenancy Deposits", Icons.Default.Security),
            Triple("received", "Payments Received", Icons.Default.Payment),
            Triple("allocations", "Payment Allocations", Icons.Default.CompareArrows),
            Triple("account", "Main Account", Icons.Default.AccountBalanceWallet)
        )
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Logo & Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(HighDensityPrimaryContainer, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = HighDensityPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "JEBLANDLORD",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityPrimary,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "Landlord Portal",
                    fontSize = 10.sp,
                    color = HighDensitySecondaryText,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Divider(color = HighDensityOutline, modifier = Modifier.padding(bottom = 12.dp))

        sections.forEach { (sectionHeader, items) ->
            Text(
                text = sectionHeader.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = HighDensitySecondaryText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                letterSpacing = 1.sp
            )

            items.forEach { (id, label, icon) ->
                val isSelected = activeView == id
                val bg = if (isSelected) HighDensitySecondaryContainer else Color.Transparent
                val borderCol = if (isSelected) HighDensityPrimary else Color.Transparent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                        .clickable { onViewSelected(id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) HighDensityPrimary else HighDensitySecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) HighDensityPrimary else HighDensityText
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// 4. View Renderer
@Composable
fun RenderActiveView(
    view: String,
    searchText: String,
    houses: List<House>,
    tenants: List<Tenant>,
    rentBillings: SnapshotStateList<RentBilling>,
    waterBillings: SnapshotStateList<WaterBilling>,
    paymentsReceived: SnapshotStateList<PaymentReceived>,
    allocations: SnapshotStateList<Allocation>,
    totalRentExpected: Int,
    totalCollected: Int,
    totalArrears: Int,
    totalWaterBilled: Int,
    occupiedUnits: Int,
    vacantUnits: Int,
    unallocatedPaymentsCount: Int,
    rentCollectedPercent: Int,
    trendData: List<Int>,
    trendMonths: List<String>,
    sentMessages: SnapshotStateList<SentMessage>,
    meterReplacements: SnapshotStateList<MeterReplacement>,
    expenses: SnapshotStateList<Expense>,
    defaultWaterRate: Int,
    onDefaultWaterRateChange: (Int) -> Unit,
    latePaymentPenaltyPercent: Int,
    onLatePaymentPenaltyPercentChange: (Int) -> Unit,
    billingDayOfMonth: Int,
    onBillingDayOfMonthChange: (Int) -> Unit,
    onNavigate: (String) -> Unit,
    onAddTenantClick: () -> Unit,
    onAddHouseClick: () -> Unit,
    onAddPaymentClick: () -> Unit,
    onManageHouseClick: (House) -> Unit,
    deposits: SnapshotStateList<DepositEntry>,
    depositTransactions: SnapshotStateList<DepositTransaction>,
    landlords: SnapshotStateList<Landlord>,
    clientRequests: SnapshotStateList<ClientRequest>,
    onAddLandlordClick: () -> Unit,
    onUpdateClientRequestStatus: (String, String) -> Unit
) {
    when (view) {
        "saas_manager" -> SaaSManagerView(
            landlords = landlords,
            clientRequests = clientRequests,
            houses = houses,
            onAddLandlordClick = onAddLandlordClick,
            onUpdateClientRequestStatus = onUpdateClientRequestStatus,
            onNavigate = onNavigate
        )
        "dashboard" -> DashboardView(
            rentBillings = rentBillings,
            waterBillings = waterBillings,
            houses = houses,
            tenants = tenants,
            totalRentExpected = totalRentExpected,
            totalCollected = totalCollected,
            totalArrears = totalArrears,
            totalWaterBilled = totalWaterBilled,
            occupiedUnits = occupiedUnits,
            vacantUnits = vacantUnits,
            unallocatedPaymentsCount = unallocatedPaymentsCount,
            rentCollectedPercent = rentCollectedPercent,
            trendData = trendData,
            trendMonths = trendMonths,
            onNavigate = onNavigate,
            onAddTenantClick = onAddTenantClick,
            onAddHouseClick = onAddHouseClick,
            onAddPaymentClick = onAddPaymentClick
        )
        "tenant_portal" -> TenantPortalView(
            tenants = tenants,
            rentBillings = rentBillings,
            waterBillings = waterBillings,
            meterReplacements = meterReplacements,
            sentMessages = sentMessages,
            paymentsReceived = paymentsReceived,
            allocations = allocations,
            onNavigate = onNavigate,
            clientRequests = clientRequests
        )
        "tenants" -> TenantsView(
            tenants = tenants,
            searchText = searchText,
            onAddTenantClick = onAddTenantClick
        )
        "houses" -> HousesView(
            houses = houses,
            searchText = searchText,
            onAddHouseClick = onAddHouseClick,
            onManageHouseClick = onManageHouseClick
        )
        "billing_table" -> BillingTableView(
            rentBillings = rentBillings,
            waterBillings = waterBillings,
            sentMessages = sentMessages,
            searchText = searchText,
            onRecordPayment = { tenantName ->
                onAddPaymentClick()
            },
            onMessageTenant = { tenantName ->
                onNavigate("messages")
            }
        )
        "rent" -> RentBillingsView(
            rentBillings = rentBillings,
            searchText = searchText
        )
        "water" -> WaterBillingsView(
            waterBillings = waterBillings,
            searchText = searchText
        )
        "payment_dashboard" -> PaymentDashboardView(
            paymentsReceived = paymentsReceived,
            allocations = allocations,
            searchText = searchText,
            onAddPaymentClick = onAddPaymentClick,
            onNavigate = onNavigate
        )
        "received" -> PaymentsReceivedView(
            payments = paymentsReceived,
            searchText = searchText,
            onAddPaymentClick = onAddPaymentClick
        )
        "allocations" -> PaymentAllocationsView(
            allocations = allocations,
            searchText = searchText
        )
        "messages" -> MessagesView(
            sentMessages = sentMessages,
            tenants = tenants
        )
        "sent" -> SentMessagesView(
            sentMessages = sentMessages
        )
        "reports" -> ReportsView(
            rentBillings = rentBillings,
            waterBillings = waterBillings,
            houses = houses
        )
        "meters" -> MetersView(
            meterReplacements = meterReplacements,
            houses = houses
        )
        "config" -> ConfigView(
            defaultWaterRate = defaultWaterRate,
            onDefaultWaterRateChange = onDefaultWaterRateChange,
            latePaymentPenaltyPercent = latePaymentPenaltyPercent,
            onLatePaymentPenaltyPercentChange = onLatePaymentPenaltyPercentChange,
            billingDayOfMonth = billingDayOfMonth,
            onBillingDayOfMonthChange = onBillingDayOfMonthChange,
            tenants = tenants,
            rentBillings = rentBillings,
            waterBillings = waterBillings,
            sentMessages = sentMessages
        )
        "deposits" -> DepositsView(
            deposits = deposits,
            depositTransactions = depositTransactions,
            tenants = tenants,
            houses = houses
        )
        "account" -> AccountView(
            expenses = expenses,
            rentCollected = totalCollected,
            waterCollected = waterBillings.filter { it.status == "Paid" }.sumOf { it.amount }
        )
        else -> ComingSoonScreen(view = view)
    }
}

// 5. Dashboard View Screen
@Composable
fun DashboardView(
    rentBillings: List<RentBilling>,
    waterBillings: SnapshotStateList<WaterBilling>,
    houses: List<House>,
    tenants: List<Tenant> = emptyList(),
    totalRentExpected: Int,
    totalCollected: Int,
    totalArrears: Int,
    totalWaterBilled: Int,
    occupiedUnits: Int,
    vacantUnits: Int,
    unallocatedPaymentsCount: Int,
    rentCollectedPercent: Int,
    trendData: List<Int>,
    trendMonths: List<String>,
    onNavigate: (String) -> Unit,
    onAddTenantClick: () -> Unit,
    onAddHouseClick: () -> Unit,
    onAddPaymentClick: () -> Unit
) {
    var reminderSentTo by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("dashboard_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (reminderSentTo != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = HighDensitySuccess.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, HighDensitySuccess),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = HighDensitySuccess,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reminder SMS generated for $reminderSentTo successfully!",
                                fontSize = 12.sp,
                                color = HighDensitySuccess,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { reminderSentTo = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close alert",
                                tint = HighDensitySuccess,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Maintenance Alert Banner
        item {
            val maintenanceIssues = houses.count { it.maintenanceStatus != "Operational" }
            if (maintenanceIssues > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("houses") }
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = HighDensityError.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, HighDensityError.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Maintenance alerts",
                                tint = HighDensityError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "$maintenanceIssues Unit Maintenance Alerts",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityText
                                )
                                Text(
                                    text = "Tap to view and resolve active property maintenance items.",
                                    fontSize = 10.sp,
                                    color = HighDensitySecondaryText
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Navigate to Units",
                            tint = HighDensitySecondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Quick high-density counters grid
        item {
            Column {
                Text(
                    text = "Nakuru Property Statistics",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensitySecondaryText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardStatCard(
                            label = "Expected Rent",
                            value = "KSh ${totalRentExpected / 1000}k",
                            subText = "July Billings",
                            accentColor = HighDensityPrimary
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardStatCard(
                            label = "Collected",
                            value = "KSh ${totalCollected / 1000}k",
                            subText = "$rentCollectedPercent% overall",
                            accentColor = HighDensitySuccess
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardStatCard(
                            label = "Arrears",
                            value = "KSh ${totalArrears / 1000}k",
                            subText = "Pending",
                            accentColor = HighDensityError
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DashboardStatCard(
                        label = "Water Charge",
                        value = "KSh ${totalWaterBilled / 1000}k",
                        subText = "Metered Units",
                        accentColor = HighDensityWaterBlue
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    DashboardStatCard(
                        label = "Occupied",
                        value = "$occupiedUnits",
                        subText = "Active Leases",
                        accentColor = HighDensityPrimary
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    DashboardStatCard(
                        label = "Vacant",
                        value = "$vacantUnits",
                        subText = "Available Units",
                        accentColor = HighDensityWarning
                    )
                }
            }
        }

        // Collection Droplet Gauge & Chart Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, HighDensityOutline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Collection Liquid Gauge", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                        Text(
                            text = "Represents overall July rent captured across units",
                            fontSize = 11.sp,
                            color = HighDensitySecondaryText,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WaterDropletGauge(percent = rentCollectedPercent.toFloat())
                        Column {
                            Text(
                                text = "$rentCollectedPercent%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityPrimary,
                                fontFamily = FontFamily.Serif
                            )
                            Text(text = "Captured on book", fontSize = 11.sp, color = HighDensitySecondaryText)
                        }
                    }
                }
            }
        }

        // Rent Arrears Monitoring Dashboard Component
        item {
            RentArrearsMonitorComponent(
                rentBillings = rentBillings,
                tenants = tenants,
                onSendReminder = { billing ->
                    reminderSentTo = billing.tenant
                }
            )
        }

        // Water Billing Tracking & Calculator Dashboard Component
        item {
            WaterBillingTrackerComponent(
                waterBillings = waterBillings,
                houses = houses
            )
        }

        // Custom Graphics Trend Area Chart replacement
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, HighDensityOutline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Receipting Trend (Thousands)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                    Text(
                        text = "Aggregated monthly rent collection profile",
                        fontSize = 11.sp,
                        color = HighDensitySecondaryText,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // Line representation bar
                    CustomTrendChart(months = trendMonths, values = trendData)
                }
            }
        }

        // Compliance Zones Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, HighDensityOutline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Tenant Compliance Zones", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                    Spacer(modifier = Modifier.height(8.dp))
                    ComplianceZoneRow(label = "Compliant status (Immediate)", count = 22, color = HighDensitySuccess)
                    ComplianceZoneRow(label = "5 – 15 days arrears", count = 4, color = HighDensityWarning)
                    ComplianceZoneRow(label = "More than 15 days arrears", count = 7, color = HighDensityError)
                }
            }
        }

        // Quick Actions panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, HighDensityOutline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Quick Landlord Commands", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickActionItem(label = "Add Tenant", icon = Icons.Default.PersonAdd, onClick = onAddTenantClick)
                        QuickActionItem(label = "Add House", icon = Icons.Default.AddHome, onClick = onAddHouseClick)
                        QuickActionItem(label = "Pay Ledger", icon = Icons.Default.PostAdd, onClick = onAddPaymentClick)
                        QuickActionItem(label = "Review Alloc", icon = Icons.Default.PlaylistAddCheck, onClick = { onNavigate("allocations") })
                    }
                }
            }
        }
    }
}

// Rent & Arrears Monitoring Dashboard Component
@Composable
fun RentArrearsMonitorComponent(
    rentBillings: List<RentBilling>,
    tenants: List<Tenant> = emptyList(),
    onSendReminder: (RentBilling) -> Unit
) {
    var selectedRiskFilter by remember { mutableStateOf<String>("All") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, HighDensityOutline),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Rent Collection & Arrears Monitor",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityText
                    )
                    Text(
                        text = "Real-time arrears aging profiles and risk classifications",
                        fontSize = 11.sp,
                        color = HighDensitySecondaryText,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .background(HighDensityError.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable { selectedRiskFilter = "All" }
                ) {
                    Text(
                        text = if (selectedRiskFilter == "All") "Arrears Alert" else "Show All",
                        color = HighDensityError,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val overdueItems = rentBillings.filter { it.balance > 0 }
            val totalOutstanding = overdueItems.sumOf { it.balance }

            // Dynamic risk breakdown calculations
            val highRiskItems = overdueItems.filter { item ->
                tenants.find { it.name == item.tenant }?.status == ">15 days"
            }
            val highRiskSum = highRiskItems.sumOf { it.balance }

            val medRiskItems = overdueItems.filter { item ->
                tenants.find { it.name == item.tenant }?.status == "5–15 days"
            }
            val medRiskSum = medRiskItems.sumOf { it.balance }

            // 1. VISUAL SEGMENTED RISK CHART (ARREARS AGING PROFILE)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HighDensitySurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "VISUAL ARREARS AGING PROFILE (CLICK SEGMENTS TO FILTER)",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensitySecondaryText,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (totalOutstanding > 0) {
                    val highPct = highRiskSum.toFloat() / totalOutstanding.toFloat()
                    val medPct = medRiskSum.toFloat() / totalOutstanding.toFloat()

                    // Proportional bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        if (highPct > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(maxOf(0.01f, highPct))
                                    .fillMaxHeight()
                                    .background(HighDensityError)
                                    .clickable { selectedRiskFilter = "High" }
                            )
                        }
                        if (medPct > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(maxOf(0.01f, medPct))
                                    .fillMaxHeight()
                                    .background(HighDensityWarning)
                                    .clickable { selectedRiskFilter = "Medium" }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Legend & Quick Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // High risk legend
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { selectedRiskFilter = "High" }
                                .background(
                                    if (selectedRiskFilter == "High") HighDensityError.copy(alpha = 0.12f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(HighDensityError, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("High (>15 Days): KSh ${String.format("%,d", highRiskSum)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                        }

                        // Medium risk legend
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { selectedRiskFilter = "Medium" }
                                .background(
                                    if (selectedRiskFilter == "Medium") HighDensityWarning.copy(alpha = 0.12f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(HighDensityWarning, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Medium (5-15 Days): KSh ${String.format("%,d", medRiskSum)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                        }
                    }
                } else {
                    Text("No arrears recorded.", fontSize = 11.sp, color = HighDensitySuccess, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-metrics/Highlight of outstanding arrears
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HighDensitySurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Outstanding Balance",
                        fontSize = 11.sp,
                        color = HighDensitySecondaryText,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "KSh ${String.format("%,d", totalOutstanding)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityError,
                        fontFamily = FontFamily.Serif
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Overdue Accounts",
                        fontSize = 11.sp,
                        color = HighDensitySecondaryText,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${overdueItems.size} Units Overdue",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val currentFilterTitle = when(selectedRiskFilter) {
                "High" -> "HIGH RISK OVERDUE UNITS DETAIL"
                "Medium" -> "MEDIUM RISK OVERDUE UNITS DETAIL"
                else -> "OVERDUE UNITS DETAIL"
            }

            Text(
                text = currentFilterTitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = HighDensitySecondaryText,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val displayItems = when (selectedRiskFilter) {
                "High" -> highRiskItems
                "Medium" -> medRiskItems
                else -> overdueItems
            }

            if (displayItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedRiskFilter == "All") "All units are compliant! No outstanding arrears." else "No units match this risk filter.",
                        fontSize = 12.sp,
                        color = HighDensitySuccess,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayItems.forEach { billing ->
                        OverdueUnitItem(billing = billing, onSendReminder = { onSendReminder(billing) })
                    }
                }
            }
        }
    }
}

@Composable
fun OverdueUnitItem(billing: RentBilling, onSendReminder: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HighDensityOutline, RoundedCornerShape(10.dp))
            .background(HighDensityBg)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = billing.unit,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityText
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(HighDensityError.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Pending",
                        color = HighDensityError,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "Tenant: ${billing.tenant}",
                fontSize = 11.sp,
                color = HighDensitySecondaryText,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Billed: KSh ${billing.billed}",
                    fontSize = 10.sp,
                    color = HighDensitySecondaryText
                )
                Text(
                    text = "Paid: KSh ${billing.paid}",
                    fontSize = 10.sp,
                    color = HighDensitySuccess
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "Arrears",
                fontSize = 10.sp,
                color = HighDensitySecondaryText
            )
            Text(
                text = "KSh ${String.format("%,d", billing.balance)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = HighDensityError,
                fontFamily = FontFamily.Serif
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onSendReminder,
                colors = ButtonDefaults.textButtonColors(contentColor = HighDensityPrimary),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "Remind", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Water Billing Tracking & Calculator Dashboard Component
@Composable
fun WaterBillingTrackerComponent(
    waterBillings: SnapshotStateList<WaterBilling>,
    houses: List<House>,
    modifier: Modifier = Modifier
) {
    // Calculator States
    var selectedUnit by remember { mutableStateOf(houses.firstOrNull()?.let { "${it.property} – ${it.unit}" } ?: "Amani Court – A1") }
    var previousReadingStr by remember { mutableStateOf("") }
    var currentReadingStr by remember { mutableStateOf("") }
    var rateStr by remember { mutableStateOf("120") }
    
    // Autopopulate previous reading if unit is selected and exists in waterBillings
    LaunchedEffect(selectedUnit) {
        val existing = waterBillings.find { it.unit == selectedUnit }
        if (existing != null) {
            previousReadingStr = existing.currReading.toString() // current reading becomes next previous reading
        } else {
            previousReadingStr = "0"
        }
        currentReadingStr = ""
    }

    val prevVal = previousReadingStr.toIntOrNull() ?: 0
    val currVal = currentReadingStr.toIntOrNull() ?: 0
    val rateVal = rateStr.toIntOrNull() ?: 120
    val calculatedUsage = if (currVal >= prevVal) currVal - prevVal else 0
    val calculatedAmount = calculatedUsage * rateVal

    // Summary calculations
    val totalConsumption = waterBillings.sumOf { it.units }
    val unpaidBillings = waterBillings.filter { it.status == "Unpaid" }
    val totalUnpaidAmount = unpaidBillings.sumOf { it.amount }
    val totalPaidAmount = waterBillings.filter { it.status == "Paid" }.sumOf { it.amount }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, HighDensityOutline),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(HighDensityPrimary.copy(alpha = 0.1f), CircleShape)
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Opacity,
                            contentDescription = "Water tracking icon",
                            tint = HighDensityPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Water Billing & Consumption",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityText
                        )
                        Text(
                            text = "Track water cycles & compute consumption",
                            fontSize = 11.sp,
                            color = HighDensitySecondaryText
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .background(HighDensityPrimary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Nakuru Water",
                        color = HighDensityPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick stats summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HighDensitySurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Total Consumed", fontSize = 10.sp, color = HighDensitySecondaryText)
                    Text(text = "$totalConsumption m³", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                }
                Column {
                    Text(text = "Paid Water", fontSize = 10.sp, color = HighDensitySecondaryText)
                    Text(text = "KSh ${String.format("%,d", totalPaidAmount)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HighDensitySuccess)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Outstanding Water", fontSize = 10.sp, color = HighDensitySecondaryText)
                    Text(text = "KSh ${String.format("%,d", totalUnpaidAmount)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HighDensityError)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Water Calculator
            Text(
                text = "UTILITY USAGE CALCULATOR",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = HighDensitySecondaryText,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = HighDensityBg),
                border = BorderStroke(1.dp, HighDensityOutline),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Unit selector (interactive row of chips)
                    Text(
                        text = "Select Unit to Update",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityText,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        houses.forEach { h ->
                            val unitStr = "${h.property} – ${h.unit}"
                            val isSelected = selectedUnit == unitStr
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) HighDensityPrimary else Color.White,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) HighDensityPrimary else HighDensityOutline,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedUnit = unitStr }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = h.unit,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else HighDensityText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Inputs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Previous Reading
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Prev Reading (m³)", fontSize = 11.sp, color = HighDensitySecondaryText, fontWeight = FontWeight.Medium)
                            OutlinedTextField(
                                value = previousReadingStr,
                                onValueChange = { previousReadingStr = it },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = HighDensityPrimary,
                                    unfocusedBorderColor = HighDensityOutline
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                            )
                        }
                        
                        // Current Reading
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Curr Reading (m³)", fontSize = 11.sp, color = HighDensitySecondaryText, fontWeight = FontWeight.Medium)
                            OutlinedTextField(
                                value = currentReadingStr,
                                onValueChange = { currentReadingStr = it },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = HighDensityPrimary,
                                    unfocusedBorderColor = HighDensityOutline
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                            )
                        }

                        // Rate per Unit
                        Column(modifier = Modifier.weight(0.8f)) {
                            Text(text = "Rate (KSh)", fontSize = 11.sp, color = HighDensitySecondaryText, fontWeight = FontWeight.Medium)
                            OutlinedTextField(
                                value = rateStr,
                                onValueChange = { rateStr = it },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = HighDensityPrimary,
                                    unfocusedBorderColor = HighDensityOutline
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Calculation dynamic preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HighDensityPrimary.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Selected Unit: $selectedUnit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                            Text(
                                text = "Consumption: $calculatedUsage m³ | Total: KSh ${String.format("%,d", calculatedAmount)}",
                                fontSize = 11.sp,
                                color = HighDensityPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Button(
                            onClick = {
                                if (currentReadingStr.isNotEmpty() && previousReadingStr.isNotEmpty()) {
                                    val index = waterBillings.indexOfFirst { it.unit == selectedUnit }
                                    val updatedBilling = WaterBilling(
                                        unit = selectedUnit,
                                        prevReading = prevVal,
                                        currReading = currVal,
                                        units = calculatedUsage,
                                        rate = rateVal,
                                        amount = calculatedAmount,
                                        status = "Unpaid" // Default to unpaid until cleared
                                    )
                                    if (index != -1) {
                                        waterBillings[index] = updatedBilling
                                    } else {
                                        waterBillings.add(updatedBilling)
                                    }
                                    // Reset input
                                    currentReadingStr = ""
                                }
                            },
                            enabled = currVal >= prevVal && currentReadingStr.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Save Bill", fontSize = 11.sp)
                        }
                    }

                    if (currVal < prevVal && currentReadingStr.isNotEmpty()) {
                        Text(
                            text = "⚠ Current reading must be greater than or equal to previous reading.",
                            color = HighDensityError,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Unpaid Water Bills Quick Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UNPAID WATER LEDGERS (${unpaidBillings.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensitySecondaryText,
                    letterSpacing = 1.sp
                )
                
                Text(
                    text = "Click checkmark to Mark as Paid",
                    fontSize = 10.sp,
                    color = HighDensitySecondaryText
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (unpaidBillings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎉 Awesome! No outstanding water bills.",
                        fontSize = 12.sp,
                        color = HighDensitySuccess,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    unpaidBillings.forEach { billing ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, HighDensityOutline, RoundedCornerShape(10.dp))
                                .background(HighDensityBg)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = billing.unit, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(HighDensityError.copy(alpha = 0.12f), CircleShape)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = "Unpaid", color = HighDensityError, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    text = "Consumption: ${billing.units} m³ (${billing.prevReading}m³ to ${billing.currReading}m³)",
                                    fontSize = 10.sp,
                                    color = HighDensitySecondaryText,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                                    Text(text = "Amount Due", fontSize = 9.sp, color = HighDensitySecondaryText)
                                    Text(text = "KSh ${String.format("%,d", billing.amount)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HighDensityError)
                                }
                                
                                IconButton(
                                    onClick = {
                                        val index = waterBillings.indexOfFirst { it.unit == billing.unit }
                                        if (index != -1) {
                                            waterBillings[index] = billing.copy(status = "Paid")
                                        }
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(HighDensitySuccess.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Mark as Paid",
                                        tint = HighDensitySuccess,
                                        modifier = Modifier.size(16.dp)
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

@Composable
fun DashboardStatCard(label: String, value: String, subText: String, accentColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, HighDensityOutline),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Accent bar at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(accentColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = HighDensityText,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(vertical = 2.dp)
            )
            Text(text = subText, fontSize = 10.sp, color = HighDensitySecondaryText)
        }
    }
}

// Custom Custom Canvas Water Droplet
@Composable
fun WaterDropletGauge(percent: Float) {
    Canvas(modifier = Modifier.size(54.dp)) {
        val width = size.width
        val height = size.height

        // Path of the water droplet
        val dropletPath = Path().apply {
            moveTo(width / 2, 0f)
            cubicTo(width / 2, 0f, width * 0.95f, height * 0.45f, width * 0.95f, height * 0.7f)
            cubicTo(width * 0.95f, height * 0.9f, width * 0.75f, height, width / 2, height)
            cubicTo(width * 0.25f, height, width * 0.05f, height * 0.9f, width * 0.05f, height * 0.7f)
            cubicTo(width * 0.05f, height * 0.45f, width * 0.5f, 0f, width / 2, 0f)
            close()
        }

        // Draw background droplet structure
        drawPath(path = dropletPath, color = HighDensitySecondaryContainer)

        // Clip the filling water according to the percent
        clipPath(path = dropletPath) {
            val fillY = height - (height * (percent / 100f))
            drawRect(
                color = HighDensityPrimary,
                topLeft = Offset(0f, fillY),
                size = size.copy(height = height * (percent / 100f))
            )
        }
    }
}

// Custom Trend Chart with lines and gradients
@Composable
fun CustomTrendChart(months: List<String>, values: List<Int>) {
    val maxValue = values.maxOrNull() ?: 150000
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            values.forEachIndexed { index, value ->
                val ratio = value.toFloat() / maxValue.toFloat()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${value / 1000}k",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.85f * ratio)
                            .width(16.dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(HighDensityPrimary, HighDensitySecondaryContainer)
                                )
                            )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            months.forEach { m ->
                Text(
                    text = m,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = HighDensitySecondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ComplianceZoneRow(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontSize = 12.sp, color = HighDensityText)
        }
        Text(
            text = "$count",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .background(color, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun QuickActionItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(HighDensitySurfaceVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = HighDensityPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
    }
}

// 6. View - Tenants list screen
@Composable
fun TenantsView(
    tenants: List<Tenant>,
    searchText: String,
    onAddTenantClick: () -> Unit
) {
    val filteredTenants = tenants.filter {
        it.name.contains(searchText, ignoreCase = true) || it.unit.contains(searchText, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "${filteredTenants.size} Active Leases", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
            Button(
                onClick = onAddTenantClick,
                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Register Tenant", fontSize = 12.sp)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredTenants) { tenant ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = tenant.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HighDensityText)
                            ComplianceBadge(status = tenant.status)
                        }
                        Text(text = "Unit: ${tenant.unit}", fontSize = 12.sp, color = HighDensitySecondaryText, modifier = Modifier.padding(top = 2.dp))
                        Text(text = "Phone: ${tenant.phone}", fontSize = 12.sp, color = HighDensitySecondaryText)
                        Divider(color = HighDensityOutline, modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Monthly Rent: KSh ${tenant.rent}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                            Text(text = "Last Check: ${tenant.lastPayment}", fontSize = 11.sp, color = HighDensitySecondaryText)
                        }
                    }
                }
            }
        }
    }
}

// 7. View - Houses list screen
@Composable
fun HousesView(
    houses: List<House>,
    searchText: String,
    onAddHouseClick: () -> Unit,
    onManageHouseClick: (House) -> Unit
) {
    var statusFilter by remember { mutableStateOf("All Statuses") }
    var maintenanceFilter by remember { mutableStateOf("All Conditions") }
    var propertyFilter by remember { mutableStateOf("All Properties") }
    
    // Dynamic lists of properties from current houses
    val propertiesList = remember(houses) {
        listOf("All Properties") + houses.map { it.property }.distinct().sorted()
    }

    // Calculations for KPIs
    val totalUnitsCount = houses.size
    val occupiedUnitsCount = houses.count { it.status == "Occupied" || it.status == "Arrears" }
    val occupancyRate = if (totalUnitsCount > 0) (occupiedUnitsCount.toFloat() / totalUnitsCount * 100f) else 0f
    val maintenanceAttentionCount = houses.count { it.maintenanceStatus != "Operational" }
    val totalPotentialYield = houses.sumOf { it.rent }

    // Filter logic
    val filteredHouses = houses.filter { house ->
        val matchesSearch = house.property.contains(searchText, ignoreCase = true) || 
                            house.unit.contains(searchText, ignoreCase = true) || 
                            house.tenant.contains(searchText, ignoreCase = true) ||
                            house.type.contains(searchText, ignoreCase = true)
        
        val matchesStatus = when (statusFilter) {
            "All Statuses" -> true
            "Vacant Only" -> house.status == "Vacant"
            "Occupied Only" -> house.status == "Occupied"
            "Arrears Only" -> house.status == "Arrears"
            else -> true
        }

        val matchesMaintenance = when (maintenanceFilter) {
            "All Conditions" -> true
            "Healthy" -> house.maintenanceStatus == "Operational"
            "Needs Attention" -> house.maintenanceStatus != "Operational"
            else -> true
        }

        val matchesProperty = if (propertyFilter == "All Properties") true else house.property == propertyFilter

        matchesSearch && matchesStatus && matchesMaintenance && matchesProperty
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // High density visual summary cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Occupancy Rate
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, HighDensityOutline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("OCCUPANCY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${String.format("%.1f", occupancyRate)}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityPrimary,
                        fontFamily = FontFamily.Serif
                    )
                    Text("$occupiedUnitsCount of $totalUnitsCount units filled", fontSize = 8.sp, color = HighDensitySecondaryText)
                }
            }

            // Card 2: Maintenance Issues
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, HighDensityOutline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("MAINTENANCE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$maintenanceAttentionCount Issues",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (maintenanceAttentionCount > 0) HighDensityError else HighDensitySuccess,
                        fontFamily = FontFamily.Serif
                    )
                    Text("Needs prompt attention", fontSize = 8.sp, color = HighDensitySecondaryText)
                }
            }

            // Card 3: Monthly Potential Yield
            Card(
                modifier = Modifier.weight(1.2f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, HighDensityOutline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("EST. MONTHLY YIELD", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "KSh ${String.format("%,d", totalPotentialYield)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensitySecondary,
                        fontFamily = FontFamily.Serif
                    )
                    Text("Gross portfolio rent capacity", fontSize = 8.sp, color = HighDensitySecondaryText)
                }
            }
        }

        // Multi-level Filters Toolbar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensitySurfaceVariant),
            border = BorderStroke(1.dp, HighDensityOutline),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Row 1: Property Filter
                Text("FILTER BY PROPERTY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    propertiesList.forEach { prop ->
                        val isSelected = propertyFilter == prop
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) HighDensityPrimary else Color.White, RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSelected) HighDensityPrimary else HighDensityOutline, RoundedCornerShape(6.dp))
                                .clickable { propertyFilter = prop }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = prop,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else HighDensityText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 2: Status & Condition Filters Side-by-Side
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("OCCUPANCY STATUS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("All Statuses", "Vacant Only", "Occupied Only", "Arrears Only").forEach { s ->
                                val isSelected = statusFilter == s
                                Box(
                                    modifier = Modifier
                                        .background(if (isSelected) HighDensityPrimary else Color.White, RoundedCornerShape(6.dp))
                                        .border(1.dp, if (isSelected) HighDensityPrimary else HighDensityOutline, RoundedCornerShape(6.dp))
                                        .clickable { statusFilter = s }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(s.replace(" Only", ""), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else HighDensityText)
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("MAINTENANCE STATUS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("All Conditions", "Healthy", "Needs Attention").forEach { m ->
                                val isSelected = maintenanceFilter == m
                                Box(
                                    modifier = Modifier
                                        .background(if (isSelected) HighDensityPrimary else Color.White, RoundedCornerShape(6.dp))
                                        .border(1.dp, if (isSelected) HighDensityPrimary else HighDensityOutline, RoundedCornerShape(6.dp))
                                        .clickable { maintenanceFilter = m }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(m, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else HighDensityText)
                                }
                            }
                        }
                    }
                }
            }
        }

        // List Header and Add Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredHouses.size} Matching Units",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = HighDensityText
            )
            Button(
                onClick = onAddHouseClick,
                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Unit", fontSize = 12.sp)
            }
        }

        // Lazy list of matching houses with expand/collapse details
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (filteredHouses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = HighDensitySurfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(32.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No property units match current filters.", fontSize = 12.sp, color = HighDensitySecondaryText)
                        }
                    }
                }
            } else {
                items(filteredHouses) { house ->
                    var isExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, HighDensityOutline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Primary Info Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${house.property} – ${house.unit}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = HighDensityText
                                    )
                                    Text(
                                        text = "Type: ${house.type}  |  Rent: KSh ${String.format("%,d", house.rent)}/mo",
                                        fontSize = 11.sp,
                                        color = HighDensitySecondaryText,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HouseBadge(status = house.status)
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when (house.maintenanceStatus) {
                                                    "Operational" -> HighDensitySuccess.copy(alpha = 0.12f)
                                                    "Under Maintenance" -> HighDensityError.copy(alpha = 0.12f)
                                                    else -> HighDensityWarning.copy(alpha = 0.12f)
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = house.maintenanceStatus,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (house.maintenanceStatus) {
                                                "Operational" -> HighDensitySuccess
                                                "Under Maintenance" -> HighDensityError
                                                else -> HighDensityWarning
                                            }
                                        )
                                    }
                                }
                            }

                            // Current tenant row (if occupied)
                            if (house.tenant != "—") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(HighDensitySurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = HighDensitySecondary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Tenant: ${house.tenant}",
                                        fontSize = 11.sp,
                                        color = HighDensityText,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Collapsible/Expanded Details Section
                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = HighDensityOutline.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("WATER METER ASSOCIATION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = HighDensitySecondary, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(house.meterNo, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("LAST MAINTENANCE CHECK", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Build, contentDescription = null, tint = HighDensitySecondaryText, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(house.lastMaintenanceDate, fontSize = 11.sp, color = HighDensityText)
                                        }
                                    }
                                }

                                if (house.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("DESK COMMENTS & SYSTEM NOTES", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                                    Text(
                                        text = house.notes,
                                        fontSize = 11.sp,
                                        color = HighDensitySecondaryText,
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .background(HighDensitySurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                            .fillMaxWidth()
                                    )
                                }
                            }

                            // Card expand indicator & Actions row
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isExpanded) "Hide details" else "Show details",
                                        tint = HighDensitySecondaryText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isExpanded) "Click to collapse" else "Click to view logs & meter",
                                        fontSize = 10.sp,
                                        color = HighDensitySecondaryText,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Quick Maintenance trigger
                                    OutlinedButton(
                                        onClick = { 
                                            // Directly open standard manage dialog where we can configure maintenance
                                            onManageHouseClick(house) 
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HighDensitySecondary),
                                        border = BorderStroke(1.dp, HighDensityOutline),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Log Work", fontSize = 10.sp)
                                    }

                                    Button(
                                        onClick = { onManageHouseClick(house) },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Manage", fontSize = 10.sp, color = Color.White)
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

// 8. View - Rent Billings ledger screen
@Composable
fun RentBillingsView(
    rentBillings: List<RentBilling>,
    searchText: String
) {
    val filteredBillings = rentBillings.filter {
        it.tenant.contains(searchText, ignoreCase = true) || it.unit.contains(searchText, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Jul 2026 Billing Ledger Cycle",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = HighDensitySecondaryText,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredBillings) { r ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = r.unit, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HighDensityText)
                            Text(text = r.month, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondary)
                        }
                        Text(text = "Tenant: ${r.tenant}", fontSize = 12.sp, color = HighDensitySecondaryText, modifier = Modifier.padding(top = 2.dp))
                        Divider(color = HighDensityOutline, modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Billed", fontSize = 10.sp, color = HighDensitySecondaryText)
                                Text("KSh ${r.billed}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                            }
                            Column {
                                Text("Paid", fontSize = 10.sp, color = HighDensitySecondaryText)
                                Text("KSh ${r.paid}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HighDensitySuccess)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Balance Due", fontSize = 10.sp, color = HighDensitySecondaryText)
                                Text(
                                    text = "KSh ${r.balance}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (r.balance > 0) HighDensityError else HighDensitySuccess
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 9. View - Water Readings list screen
@Composable
fun WaterBillingsView(
    waterBillings: List<WaterBilling>,
    searchText: String
) {
    val filteredWater = waterBillings.filter {
        it.unit.contains(searchText, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "July Water Reading ledgers",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = HighDensitySecondaryText,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredWater) { w ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = w.unit, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HighDensityText)
                            StatusBadge(status = w.status)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Previous reading", fontSize = 10.sp, color = HighDensitySecondaryText)
                                Text("${w.prevReading} m³", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                            }
                            Column {
                                Text("Current reading", fontSize = 10.sp, color = HighDensitySecondaryText)
                                Text("${w.currReading} m³", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                            }
                            Column {
                                Text("Units Used", fontSize = 10.sp, color = HighDensitySecondaryText)
                                Text("${w.units} units", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Charge (rate KSh ${w.rate})", fontSize = 10.sp, color = HighDensitySecondaryText)
                                Text("KSh ${w.amount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 10. View - Received payments
@Composable
fun PaymentsReceivedView(
    payments: List<PaymentReceived>,
    searchText: String,
    onAddPaymentClick: () -> Unit
) {
    val filteredPayments = payments.filter {
        it.tenant.contains(searchText, ignoreCase = true) || it.ref.contains(searchText, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Payments received ledger", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
            Button(
                onClick = onAddPaymentClick,
                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Record Payment", fontSize = 12.sp)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredPayments) { p ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = p.tenant, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HighDensityText)
                            Text(
                                text = if (p.allocated) "Allocated" else "Unallocated Fund",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (p.allocated) HighDensitySuccess else HighDensityWarning,
                                modifier = Modifier
                                    .background(
                                        color = if (p.allocated) HighDensitySuccess.copy(alpha = 0.15f) else HighDensityWarning.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Ref: ${p.ref} (${p.method})", fontSize = 12.sp, color = HighDensitySecondaryText)
                            Text(text = "KSh ${p.amount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                        }
                        Text(text = "Date Received: ${p.date}", fontSize = 11.sp, color = HighDensitySecondaryText, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

// 11. View - Allocations
@Composable
fun PaymentAllocationsView(
    allocations: List<Allocation>,
    searchText: String
) {
    val filteredAllocations = allocations.filter {
        it.ref.contains(searchText, ignoreCase = true) || it.suggestedUnit.contains(searchText, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Payments received ledger awaiting matching",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = HighDensitySecondaryText,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredAllocations) { a ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Transaction Ref: ${a.ref}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HighDensityText)
                            Text(text = "KSh ${a.amount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                        }
                        Text(text = "Received On: ${a.receivedOn}", fontSize = 12.sp, color = HighDensitySecondaryText, modifier = Modifier.padding(top = 2.dp))
                        Text(text = "Auto suggestion match: ${a.suggestedUnit}", fontSize = 12.sp, color = HighDensitySecondaryText)
                        Divider(color = HighDensityOutline, modifier = Modifier.padding(vertical = 8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = HighDensitySecondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Audit Note: ${a.note}", fontSize = 11.sp, color = HighDensitySecondaryText)
                        }
                    }
                }
            }
        }
    }
}

// 12. Helper UI widgets / Badges
@Composable
fun ComplianceBadge(status: String) {
    val (col, bg) = when (status) {
        "Compliant" -> HighDensitySuccess to HighDensitySuccess.copy(alpha = 0.15f)
        "5–15 days" -> HighDensityWarning to HighDensityWarning.copy(alpha = 0.15f)
        else -> HighDensityError to HighDensityError.copy(alpha = 0.15f)
    }
    Text(
        text = status,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = col,
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
fun HouseBadge(status: String) {
    val (col, bg) = when (status) {
        "Occupied" -> HighDensitySuccess to HighDensitySuccess.copy(alpha = 0.15f)
        "Vacant" -> HighDensityWarning to HighDensityWarning.copy(alpha = 0.15f)
        else -> HighDensityError to HighDensityError.copy(alpha = 0.15f)
    }
    Text(
        text = status,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = col,
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
fun StatusBadge(status: String) {
    val (col, bg) = when (status) {
        "Paid" -> HighDensitySuccess to HighDensitySuccess.copy(alpha = 0.15f)
        else -> HighDensityError to HighDensityError.copy(alpha = 0.15f)
    }
    Text(
        text = status,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = col,
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

// SaaS Landlord Onboarding Dialog
@Composable
fun AddLandlordDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, email: String, phone: String, commissionPercent: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var commissionStr by remember { mutableStateOf("10") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, HighDensityOutline)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text("Onboard New SaaS Landlord", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Landlord Business Name", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighDensityPrimary,
                        unfocusedBorderColor = HighDensityOutline
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighDensityPrimary,
                        unfocusedBorderColor = HighDensityOutline
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Contact Number", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighDensityPrimary,
                        unfocusedBorderColor = HighDensityOutline
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = commissionStr,
                    onValueChange = { commissionStr = it },
                    label = { Text("SaaS Commission Rate (%)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighDensityPrimary,
                        unfocusedBorderColor = HighDensityOutline
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = HighDensitySecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val rate = commissionStr.toIntOrNull() ?: 10
                            onConfirm(name, email, phone, rate)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Onboard Landlord", color = Color.White)
                    }
                }
            }
        }
    }
}

// 12. SaaS Landlord Manager and Reseller dashboard
@Composable
fun SaaSManagerView(
    landlords: SnapshotStateList<Landlord>,
    clientRequests: SnapshotStateList<ClientRequest>,
    houses: List<House>,
    onAddLandlordClick: () -> Unit,
    onUpdateClientRequestStatus: (String, String) -> Unit,
    onNavigate: (String) -> Unit
) {
    var managerTab by remember { mutableStateOf("landlords") } // "landlords", "requests"

    Column(modifier = Modifier.fillMaxSize().testTag("saas_manager_view")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SaaS Platform Management & Onboarding",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityText
                )
                Text(
                    text = "Manage onboarded landlords, property mapping, and service requests.",
                    fontSize = 11.sp,
                    color = HighDensitySecondaryText
                )
            }
            
            Button(
                onClick = onAddLandlordClick,
                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Onboard Landlord", fontSize = 11.sp, color = Color.White)
            }
        }

        // Sub-tabs
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("landlords" to "Onboarded Landlords", "requests" to "Client Maintenance Requests").forEach { (tabId, label) ->
                val isSelected = managerTab == tabId
                Box(
                    modifier = Modifier
                        .background(if (isSelected) HighDensitySecondaryContainer else HighDensityBg, RoundedCornerShape(8.dp))
                        .border(1.dp, if (isSelected) HighDensityPrimary else HighDensityOutline, RoundedCornerShape(8.dp))
                        .clickable { managerTab = tabId }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) HighDensityPrimary else HighDensitySecondary
                    )
                }
            }
        }

        if (managerTab == "landlords") {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Platform Quick Stats Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, HighDensityOutline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Landlords", fontSize = 10.sp, color = HighDensitySecondaryText)
                                Text("${landlords.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                            }
                            Column {
                                Text("Platform Houses", fontSize = 10.sp, color = HighDensitySecondaryText)
                                Text("${houses.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                            }
                            Column {
                                Text("Estimated ARR Fee Yield", fontSize = 10.sp, color = HighDensitySecondaryText)
                                val estimatedYield = landlords.sumOf { l ->
                                    val lHouses = houses.filter { it.landlordId == l.id }
                                    lHouses.sumOf { it.rent } * l.commissionPercent / 100
                                }
                                Text("KSh $estimatedYield/mo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                            }
                        }
                    }
                }

                // Landlords List
                items(landlords) { landlord ->
                    val landlordHouses = houses.filter { it.landlordId == landlord.id }
                    val expectedRent = landlordHouses.sumOf { it.rent }
                    val occupied = landlordHouses.count { it.status == "Occupied" || it.status == "Arrears" }
                    val vacant = landlordHouses.count { it.status == "Vacant" }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, HighDensityOutline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier.size(32.dp).background(HighDensitySecondaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = HighDensityPrimary, modifier = Modifier.size(16.dp))
                                    }
                                    Column {
                                        Text(landlord.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                                        Text("Onboarded: ${landlord.onboardingDate}", fontSize = 10.sp, color = HighDensitySecondaryText)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (landlord.status == "Active") HighDensitySuccess.copy(alpha = 0.12f) else HighDensityWarning.copy(alpha = 0.12f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = landlord.status,
                                        fontSize = 9.sp,
                                        color = if (landlord.status == "Active") HighDensitySuccess else HighDensityWarning,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Contact Details", fontSize = 9.sp, color = HighDensitySecondaryText)
                                    Text("Phone: ${landlord.phone}", fontSize = 11.sp, color = HighDensityText)
                                    Text("Email: ${landlord.email}", fontSize = 11.sp, color = HighDensityText)
                                }
                                Column {
                                    Text("Properties Summary", fontSize = 9.sp, color = HighDensitySecondaryText)
                                    Text("Units: ${landlordHouses.size} (${occupied} Occupied, ${vacant} Vacant)", fontSize = 11.sp, color = HighDensityText)
                                    Text("Expected Monthly: KSh $expectedRent", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = HighDensityOutline)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = HighDensitySecondary, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "Your Platform Cut: ${landlord.commissionPercent}% (KSh ${expectedRent * landlord.commissionPercent / 100}/mo)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = HighDensitySecondaryText
                                    )
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        onNavigate("dashboard")
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, HighDensityPrimary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Simulate Landlord View", fontSize = 10.sp, color = HighDensityPrimary)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Client Requests sub-tab
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (clientRequests.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = HighDensityBg)
                        ) {
                            Text(
                                text = "No client maintenance requests reported.",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(16.dp),
                                color = HighDensitySecondaryText
                            )
                        }
                    }
                }

                items(clientRequests) { request ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, HighDensityOutline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val icon = when (request.requestType) {
                                        "Repairs" -> Icons.Default.Build
                                        "Painting" -> Icons.Default.Brush
                                        else -> Icons.Default.Comment
                                    }
                                    val tint = when (request.requestType) {
                                        "Repairs" -> HighDensityError
                                        "Painting" -> HighDensityPrimary
                                        else -> HighDensitySecondary
                                    }
                                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "${request.requestType} request",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tint
                                    )
                                    Text(
                                        text = "• ${request.unit}",
                                        fontSize = 11.sp,
                                        color = HighDensitySecondaryText
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .background(
                                            when (request.status) {
                                                "Completed" -> HighDensitySuccess.copy(alpha = 0.12f)
                                                "Scheduled" -> HighDensityWarning.copy(alpha = 0.12f)
                                                else -> HighDensityError.copy(alpha = 0.12f)
                                            },
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = request.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (request.status) {
                                            "Completed" -> HighDensitySuccess
                                            "Scheduled" -> HighDensityWarning
                                            else -> HighDensityError
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = request.description,
                                fontSize = 12.sp,
                                color = HighDensityText
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("By: ${request.tenantName}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                                    Text("Reported: ${request.requestDate}", fontSize = 9.sp, color = HighDensitySecondaryText)
                                    if (request.resolutionNotes.isNotEmpty()) {
                                        Text("Log: ${request.resolutionNotes}", fontSize = 10.sp, color = HighDensityPrimary, fontWeight = FontWeight.Medium)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (request.status != "Scheduled" && request.status != "Completed") {
                                        OutlinedButton(
                                            onClick = { onUpdateClientRequestStatus(request.id, "Scheduled") },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Schedule", fontSize = 10.sp)
                                        }
                                    }
                                    if (request.status != "Completed") {
                                        Button(
                                            onClick = { onUpdateClientRequestStatus(request.id, "Completed") },
                                            colors = ButtonDefaults.buttonColors(containerColor = HighDensitySuccess),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Complete", fontSize = 10.sp, color = Color.White)
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
}

// 13. Dialog implementations for Landlord commands
@Composable
fun AddTenantDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, unit: String, phone: String, rent: Int) -> Unit,
    housesList: List<String>
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(housesList.firstOrNull() ?: "") }
    var phone by remember { mutableStateOf("") }
    var rentText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, HighDensityOutline)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text("Register New Tenant", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tenant Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Contact (e.g. 0722 ...)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = rentText,
                    onValueChange = { rentText = it },
                    label = { Text("Monthly Rent Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text("Assigned Property Unit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                // Simplified text input representing house dropdown selection
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Enter Unit Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = HighDensitySecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val rentVal = rentText.toIntOrNull() ?: 8000
                            onConfirm(name, unit, phone, rentVal)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                    ) {
                        Text("Add Tenant", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AddHouseDialog(
    onDismiss: () -> Unit,
    onConfirm: (property: String, unit: String, type: String, rent: Int, landlordId: String) -> Unit,
    landlords: List<Landlord>,
    initialLandlordId: String
) {
    var property by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("1BR") }
    var rentText by remember { mutableStateOf("") }
    var selectedLandlordId by remember { mutableStateOf(initialLandlordId) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, HighDensityOutline)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text("Add New Property Unit", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = property,
                    onValueChange = { property = it },
                    label = { Text("Property Name (e.g. Rongai Heights)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit Code (e.g. C3)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Unit Type (e.g. Studio, 2BR)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = rentText,
                    onValueChange = { rentText = it },
                    label = { Text("Monthly Base Rent") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))

                Text("Assign Landlord Owner", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    landlords.forEach { l ->
                        val isSelected = selectedLandlordId == l.id
                        Box(
                            modifier = Modifier
                                .background(if (isSelected) HighDensityPrimary else HighDensityBg, RoundedCornerShape(6.dp))
                                .border(1.dp, if (isSelected) HighDensityPrimary else HighDensityOutline, RoundedCornerShape(6.dp))
                                .clickable { selectedLandlordId = l.id }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = l.name.substringBefore(" "),
                                fontSize = 10.sp,
                                color = if (isSelected) Color.White else HighDensityText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = HighDensitySecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val rentVal = rentText.toIntOrNull() ?: 7500
                            onConfirm(property, unit, type, rentVal, selectedLandlordId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                    ) {
                        Text("Add Unit", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AddPaymentDialog(
    onDismiss: () -> Unit,
    onConfirm: (date: String, ref: String, tenant: String, method: String, amount: Int, allocated: Boolean) -> Unit,
    tenantsList: List<String>
) {
    var date by remember { mutableStateOf("11 Jul 2026") }
    var ref by remember { mutableStateOf("") }
    var tenant by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("M-Pesa") }
    var allocated by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, HighDensityOutline)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text("Record Incoming Payment", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = { Text("M-Pesa/Bank Ref Code (e.g. SKA9XX...)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = tenant,
                    onValueChange = { tenant = it },
                    label = { Text("Tenant Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount (KSh)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = method,
                    onValueChange = { method = it },
                    label = { Text("Payment Method (M-Pesa / Bank)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { allocated = !allocated }
                ) {
                    Checkbox(checked = allocated, onCheckedChange = { allocated = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto-allocate to tenant rent ledger", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = HighDensitySecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amountVal = amountText.toIntOrNull() ?: 5000
                            onConfirm(date, ref, tenant, method, amountVal, allocated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                    ) {
                        Text("Record Ledger", color = Color.White)
                    }
                }
            }
        }
    }
}

// 14. Custom Utility Views implemented for high-density portals
@Composable
fun MessagesView(
    sentMessages: SnapshotStateList<SentMessage>,
    tenants: List<Tenant>,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("Announcement") }
    var selectedTenant by remember { mutableStateOf("All Tenants") }
    var messageBody by remember { mutableStateOf("") }
    var showSentSnackbar by remember { mutableStateOf(false) }

    val categories = listOf("Announcement", "Rent Reminder", "Utility Notice")

    LaunchedEffect(selectedCategory, selectedTenant) {
        val targetName = if (selectedTenant == "All Tenants") "Valued Tenants" else selectedTenant.split(" (").first()
        messageBody = when (selectedCategory) {
            "Rent Reminder" -> "Dear $targetName, this is a gentle reminder that your lease installment for the current month is due. Please clear outstanding amounts at your earliest convenience."
            "Utility Notice" -> "Dear $targetName, Nakuru Water services has scheduled a maintenance cycle in our estate area. There may be low pressure today. Please store water."
            else -> "Dear $targetName, please find our weekly portal update with rent receipts and meter replacements uploaded dynamically to your estate ledger."
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(Modifier.verticalScroll(rememberScrollState()))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, HighDensityOutline),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Broadcast Tenant Notification",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityText
                )
                Text(
                    text = "Draft SMS alerts or bulk announcements to leased estates",
                    fontSize = 12.sp,
                    color = HighDensitySecondaryText,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text("MESSAGE CATEGORY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText, letterSpacing = 1.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .background(if (isSel) HighDensityPrimary else HighDensitySurfaceVariant, RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSel) HighDensityPrimary else HighDensityOutline, RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else HighDensityText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("RECIPIENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText, letterSpacing = 1.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val recipientOptions = listOf("All Tenants") + tenants.map { "${it.name} (${it.unit.split(" – ").last()})" }
                    recipientOptions.forEach { rep ->
                        val isSel = selectedTenant == rep
                        Box(
                            modifier = Modifier
                                .background(if (isSel) HighDensitySecondary else HighDensitySurfaceVariant, RoundedCornerShape(8.dp))
                                .border(1.dp, if (isSel) HighDensitySecondary else HighDensityOutline, RoundedCornerShape(8.dp))
                                .clickable { selectedTenant = rep }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(rep, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else HighDensityText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("MESSAGE BODY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText, letterSpacing = 1.sp)
                OutlinedTextField(
                    value = messageBody,
                    onValueChange = { messageBody = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighDensityPrimary,
                        unfocusedBorderColor = HighDensityOutline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        sentMessages.add(
                            0,
                            SentMessage(
                                id = "MSG-${(104..999).random()}",
                                date = "14 Jul 2026",
                                recipients = selectedTenant,
                                messageText = messageBody,
                                status = "Delivered",
                                category = selectedCategory
                            )
                        )
                        showSentSnackbar = true
                        messageBody = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Broadcast Message", fontWeight = FontWeight.Bold)
                }

                if (showSentSnackbar) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HighDensitySuccess.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, HighDensitySuccess, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = HighDensitySuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Message queued & broadcasted successfully via GSM Gateway!", color = HighDensitySuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    LaunchedEffect(showSentSnackbar) {
                        kotlinx.coroutines.delay(4000)
                        showSentSnackbar = false
                    }
                }
            }
        }
    }
}

@Composable
fun SentMessagesView(
    sentMessages: List<SentMessage>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Outbox message logs to tenant devices",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = HighDensitySecondaryText,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sentMessages) { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val (color, bg) = when (msg.category) {
                                    "Announcement" -> HighDensityPrimary to HighDensityPrimary.copy(alpha = 0.12f)
                                    "Rent Reminder" -> HighDensityWarning to HighDensityWarning.copy(alpha = 0.12f)
                                    else -> HighDensitySecondary to HighDensitySecondary.copy(alpha = 0.12f)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(bg, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(msg.category, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(msg.id, fontSize = 11.sp, color = HighDensitySecondaryText)
                            }
                            Box(
                                modifier = Modifier
                                    .background(HighDensitySuccess.copy(alpha = 0.15f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(msg.status, color = HighDensitySuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "To: ${msg.recipients}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityText,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Text(
                            text = msg.messageText,
                            fontSize = 12.sp,
                            color = HighDensitySecondaryText,
                            modifier = Modifier.padding(top = 4.dp),
                            lineHeight = 16.sp
                        )

                        Divider(color = HighDensityOutline, modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Sent Date: ${msg.date}", fontSize = 10.sp, color = HighDensitySecondaryText)
                            Text(text = "Channel: SMS Broadcast", fontSize = 10.sp, color = HighDensitySecondaryText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsView(
    rentBillings: List<RentBilling>,
    waterBillings: List<WaterBilling>,
    houses: List<House>,
    modifier: Modifier = Modifier
) {
    var isExporting by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf("") }

    val totalRentBilled = rentBillings.sumOf { it.billed }
    val totalRentCollected = rentBillings.sumOf { it.paid }
    val collectionEfficiency = if (totalRentBilled > 0) ((totalRentCollected.toFloat() / totalRentBilled.toFloat()) * 100).toInt() else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(Modifier.verticalScroll(rememberScrollState()))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, HighDensityOutline),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Consolidated Performance Audit",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityText
                )
                Text(
                    text = "Key portfolio analytics across managed real estate",
                    fontSize = 12.sp,
                    color = HighDensitySecondaryText,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(HighDensitySurfaceVariant, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text("Recovery Efficiency", fontSize = 10.sp, color = HighDensitySecondaryText)
                        Text("$collectionEfficiency%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(HighDensitySurfaceVariant, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text("Rent Collected", fontSize = 10.sp, color = HighDensitySecondaryText)
                        Text("KSh ${String.format("%,d", totalRentCollected)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HighDensitySuccess)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "💧 LEAK WATCH: TOP CONSUMING UNITS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensitySecondaryText,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    waterBillings.sortedByDescending { it.units }.take(4).forEach { bill ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, HighDensityOutline, RoundedCornerShape(10.dp))
                                .background(HighDensityBg)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(bill.unit, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                                Text("Last usage: ${bill.units} m³", fontSize = 10.sp, color = HighDensitySecondaryText)
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (bill.units >= 10) HighDensityError.copy(alpha = 0.12f) else HighDensitySuccess.copy(alpha = 0.12f),
                                        CircleShape
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (bill.units >= 10) "High Consumption" else "Normal",
                                    color = if (bill.units >= 10) HighDensityError else HighDensitySuccess,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "EXPORT OPERATIONAL LOGS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensitySecondaryText,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            isExporting = true
                            exportStatus = "Building PDF compilation layout..."
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Audit PDF", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            isExporting = true
                            exportStatus = "Generating spreadsheet entries (CSV)..."
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensitySecondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Excel (CSV)", fontSize = 11.sp)
                    }
                }

                if (isExporting) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HighDensityPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .border(1.dp, HighDensityPrimary, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = HighDensityPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(exportStatus, color = HighDensityPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    LaunchedEffect(isExporting) {
                        kotlinx.coroutines.delay(2000)
                        exportStatus = "Success! File downloaded to /Download folder."
                        kotlinx.coroutines.delay(2000)
                        isExporting = false
                    }
                }
            }
        }
    }
}

@Composable
fun MetersView(
    meterReplacements: SnapshotStateList<MeterReplacement>,
    houses: List<House>,
    modifier: Modifier = Modifier
) {
    var selectedUnit by remember { mutableStateOf(houses.firstOrNull()?.unit ?: "A1") }
    var replacementReason by remember { mutableStateOf("") }
    var estimatedCostStr by remember { mutableStateOf("4500") }
    var showSuccess by remember { mutableStateOf(false) }

    val reasons = listOf("Faulty reading", "Physical damage", "Leaking seal")

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(Modifier.verticalScroll(rememberScrollState()))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, HighDensityOutline),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Water Meter Replacement Logs",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityText
                )
                Text(
                    text = "Request hardware updates for malfunctioning utility meters",
                    fontSize = 12.sp,
                    color = HighDensitySecondaryText,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = HighDensityBg),
                    border = BorderStroke(1.dp, HighDensityOutline),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("LOG A REPLACEMENT REQUEST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText, letterSpacing = 1.sp)
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Select Estate Unit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            houses.forEach { h ->
                                val isSel = selectedUnit == h.unit
                                Box(
                                    modifier = Modifier
                                        .background(if (isSel) HighDensityPrimary else Color.White, RoundedCornerShape(8.dp))
                                        .border(1.dp, if (isSel) HighDensityPrimary else HighDensityOutline, RoundedCornerShape(8.dp))
                                        .clickable { selectedUnit = h.unit }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(h.unit, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else HighDensityText)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Select Reason", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            reasons.forEach { r ->
                                val isSel = replacementReason == r
                                Box(
                                    modifier = Modifier
                                        .background(if (isSel) HighDensitySecondary else Color.White, RoundedCornerShape(8.dp))
                                        .border(1.dp, if (isSel) HighDensitySecondary else HighDensityOutline, RoundedCornerShape(8.dp))
                                        .clickable { replacementReason = r }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(r, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else HighDensityText)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = estimatedCostStr,
                                onValueChange = { estimatedCostStr = it },
                                label = { Text("Est Cost (KSh)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HighDensityPrimary,
                                    unfocusedBorderColor = HighDensityOutline
                                )
                            )

                            Button(
                                onClick = {
                                    val costVal = estimatedCostStr.toIntOrNull() ?: 4500
                                    val fullPropName = houses.find { it.unit == selectedUnit }?.let { "${it.property} – ${it.unit}" } ?: "Amani Court – $selectedUnit"
                                    meterReplacements.add(
                                        0,
                                        MeterReplacement(
                                            id = "MTR-${(503..999).random()}",
                                            unit = fullPropName,
                                            requestDate = "14 Jul 2026",
                                            reason = if (replacementReason.isNotEmpty()) replacementReason else "Malfunctioning",
                                            cost = costVal,
                                            status = "Pending Approval"
                                        )
                                    )
                                    showSuccess = true
                                    replacementReason = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log Replacement", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (showSuccess) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(HighDensitySuccess.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .border(1.dp, HighDensitySuccess, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = HighDensitySuccess, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Replacement log compiled successfully!", color = HighDensitySuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            LaunchedEffect(showSuccess) {
                                kotlinx.coroutines.delay(3000)
                                showSuccess = false
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "REPLACEMENT HISTORY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensitySecondaryText,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    meterReplacements.forEach { rep ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, HighDensityOutline, RoundedCornerShape(10.dp))
                                .background(HighDensityBg)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(rep.unit, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val (color, bg) = when (rep.status) {
                                        "Completed" -> HighDensitySuccess to HighDensitySuccess.copy(alpha = 0.15f)
                                        else -> HighDensityWarning to HighDensityWarning.copy(alpha = 0.15f)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(bg, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(rep.status, color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("Reason: ${rep.reason}", fontSize = 11.sp, color = HighDensitySecondaryText, modifier = Modifier.padding(top = 2.dp))
                                Text("Date logged: ${rep.requestDate}", fontSize = 10.sp, color = HighDensitySecondaryText)
                            }
                            Text("KSh ${String.format("%,d", rep.cost)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigView(
    defaultWaterRate: Int,
    onDefaultWaterRateChange: (Int) -> Unit,
    latePaymentPenaltyPercent: Int,
    onLatePaymentPenaltyPercentChange: (Int) -> Unit,
    billingDayOfMonth: Int,
    onBillingDayOfMonthChange: (Int) -> Unit,
    tenants: List<Tenant> = emptyList(),
    rentBillings: SnapshotStateList<RentBilling> = SnapshotStateList(),
    waterBillings: SnapshotStateList<WaterBilling> = SnapshotStateList(),
    sentMessages: SnapshotStateList<SentMessage> = SnapshotStateList(),
    modifier: Modifier = Modifier
) {
    var waterRateStr by remember { mutableStateOf(defaultWaterRate.toString()) }
    var penaltyStr by remember { mutableStateOf(latePaymentPenaltyPercent.toString()) }
    var billingDayStr by remember { mutableStateOf(billingDayOfMonth.toString()) }
    var isSaved by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(Modifier.verticalScroll(rememberScrollState()))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, HighDensityOutline),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Estate Ledger Global Config",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityText
                )
                Text(
                    text = "Configure parameters dynamically applied across all calculation tools",
                    fontSize = 12.sp,
                    color = HighDensitySecondaryText,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text("Default Water Unit Rate (KSh per m³)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                        OutlinedTextField(
                            value = waterRateStr,
                            onValueChange = { waterRateStr = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HighDensityPrimary,
                                unfocusedBorderColor = HighDensityOutline
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                        )
                    }

                    Column {
                        Text("Late Rent Payment Penalty (%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                        OutlinedTextField(
                            value = penaltyStr,
                            onValueChange = { penaltyStr = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HighDensityPrimary,
                                unfocusedBorderColor = HighDensityOutline
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                        )
                    }

                    Column {
                        Text("Default Lease Billing Day (Day of Month)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                        OutlinedTextField(
                            value = billingDayStr,
                            onValueChange = { billingDayStr = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HighDensityPrimary,
                                unfocusedBorderColor = HighDensityOutline
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val waterRate = waterRateStr.toIntOrNull() ?: 120
                        val penalty = penaltyStr.toIntOrNull() ?: 5
                        val billingDay = billingDayStr.toIntOrNull() ?: 5
                        onDefaultWaterRateChange(waterRate)
                        onLatePaymentPenaltyPercentChange(penalty)
                        onBillingDayOfMonthChange(billingDay)
                        isSaved = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Global System Parameters", fontWeight = FontWeight.Bold)
                }

                if (isSaved) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HighDensitySuccess.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, HighDensitySuccess, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = HighDensitySuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("System configurations updated & re-indexed globally!", color = HighDensitySuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    LaunchedEffect(isSaved) {
                        kotlinx.coroutines.delay(4000)
                        isSaved = false
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Automated Alerts Scheduler & Triggers Card
        var autoRentInvoice by remember { mutableStateOf(true) }
        var autoOverdueWarning by remember { mutableStateOf(true) }
        var autoWaterSpikeAlert by remember { mutableStateOf(true) }
        var isAlertRunning by remember { mutableStateOf(false) }
        var alertProgressMessage by remember { mutableStateOf("") }
        var alertSummaryMessage by remember { mutableStateOf<String?>(null) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, HighDensityOutline),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Automated Email Alerts Scheduler",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityText
                )
                Text(
                    text = "Configure rules to automatically dispatch email alerts to your active tenants",
                    fontSize = 12.sp,
                    color = HighDensitySecondaryText,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Trigger Rule 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Monthly Lease Invoices", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                        Text("Send automated rent & water invoice on lease billing day", fontSize = 11.sp, color = HighDensitySecondaryText)
                    }
                    Switch(
                        checked = autoRentInvoice,
                        onCheckedChange = { autoRentInvoice = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = HighDensityPrimary,
                            uncheckedThumbColor = HighDensitySecondaryText,
                            uncheckedTrackColor = HighDensitySurfaceVariant
                        )
                    )
                }

                HorizontalDivider(color = HighDensityOutline.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                // Trigger Rule 2
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Overdue Rent Warnings", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                        Text("Escalate via email to tenants whose balances are outstanding", fontSize = 11.sp, color = HighDensitySecondaryText)
                    }
                    Switch(
                        checked = autoOverdueWarning,
                        onCheckedChange = { autoOverdueWarning = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = HighDensityPrimary,
                            uncheckedThumbColor = HighDensitySecondaryText,
                            uncheckedTrackColor = HighDensitySurfaceVariant
                        )
                    )
                }

                HorizontalDivider(color = HighDensityOutline.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                // Trigger Rule 3
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Water Leakage warnings", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                        Text("Send notice if monthly water units spike > 10m³", fontSize = 11.sp, color = HighDensitySecondaryText)
                    }
                    Switch(
                        checked = autoWaterSpikeAlert,
                        onCheckedChange = { autoWaterSpikeAlert = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = HighDensityPrimary,
                            uncheckedThumbColor = HighDensitySecondaryText,
                            uncheckedTrackColor = HighDensitySurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Alert Engine Status Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HighDensitySurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = HighDensityPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Engine Status: ACTIVE & MONITORING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Trigger Button
                Button(
                    onClick = {
                        isAlertRunning = true
                        alertSummaryMessage = null
                        alertProgressMessage = "Initializing automated scheduler..."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensitySecondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger Automated Email Alerts Batch", fontWeight = FontWeight.Bold)
                }

                if (isAlertRunning) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HighDensitySecondary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .border(1.dp, HighDensitySecondary, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = HighDensitySecondary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(alertProgressMessage, color = HighDensitySecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    LaunchedEffect(isAlertRunning) {
                        // Simulated background engine execution
                        kotlinx.coroutines.delay(1000)
                        alertProgressMessage = "Scanning lease books for outstanding balances..."
                        kotlinx.coroutines.delay(1200)

                        var rentRemindersCount = 0
                        var leakWarningsCount = 0

                        // 1. Check for outstanding rent
                        if (autoOverdueWarning) {
                            rentBillings.forEach { billing ->
                                if (billing.balance > 0) {
                                    sentMessages.add(
                                        0,
                                        SentMessage(
                                            id = "ALERT-${(1000..9999).random()}",
                                            date = "14 Jul 2026",
                                            recipients = "${billing.tenant} (${billing.unit.split(" – ").last()})",
                                            messageText = "Dear ${billing.tenant}, our automated ledger shows an outstanding rent balance of KSh ${billing.balance} for July 2026. Please settle before late fees apply.",
                                            status = "Emailed & Delivered",
                                            category = "Rent Reminder"
                                        )
                                    )
                                    rentRemindersCount++
                                }
                            }
                        }

                        // 2. Check for water usage leaks (> 10 units)
                        if (autoWaterSpikeAlert) {
                            waterBillings.forEach { billing ->
                                if (billing.units >= 10) {
                                    val matchTenant = tenants.find { it.unit.contains(billing.unit) }?.name ?: "Valued Tenant"
                                    sentMessages.add(
                                        0,
                                        SentMessage(
                                            id = "ALERT-${(1000..9999).random()}",
                                            date = "14 Jul 2026",
                                            recipients = "$matchTenant (${billing.unit})",
                                            messageText = "Dear $matchTenant, our smart metering detected an unusually high water usage of ${billing.units}m³ this month. Please check for leaks.",
                                            status = "Emailed & Delivered",
                                            category = "Utility Notice"
                                        )
                                    )
                                    leakWarningsCount++
                                }
                            }
                        }

                        alertProgressMessage = "Compiling and dispatching personalized alerts..."
                        kotlinx.coroutines.delay(1000)

                        alertSummaryMessage = "Job finished! Sent $rentRemindersCount rent alerts and $leakWarningsCount water warnings."
                        isAlertRunning = false
                    }
                }

                alertSummaryMessage?.let { summary ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HighDensitySuccess.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, HighDensitySuccess, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, tint = HighDensitySuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(summary, color = HighDensitySuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountView(
    expenses: SnapshotStateList<Expense>,
    rentCollected: Int,
    waterCollected: Int,
    modifier: Modifier = Modifier
) {
    var expenseCategory by remember { mutableStateOf("Plumbing") }
    var expenseDesc by remember { mutableStateOf("") }
    var expenseAmountStr by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }

    val categories = listOf("Plumbing", "Electrical", "Cleaning", "Nakuru Water", "Tax")

    val totalIncome = rentCollected + waterCollected
    val totalExpenses = expenses.sumOf { it.amount }
    val netProfit = totalIncome - totalExpenses

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(Modifier.verticalScroll(rememberScrollState()))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, HighDensityOutline),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Consolidated Operating Ledger",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityText
                )
                Text(
                    text = "Review operating cashflows and net portfolio yield",
                    fontSize = 12.sp,
                    color = HighDensitySecondaryText,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HighDensitySurfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Gross Income", fontSize = 10.sp, color = HighDensitySecondaryText)
                        Text(text = "KSh ${String.format("%,d", totalIncome)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HighDensitySuccess)
                    }
                    Column {
                        Text(text = "Operating Expenses", fontSize = 10.sp, color = HighDensitySecondaryText)
                        Text(text = "KSh ${String.format("%,d", totalExpenses)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HighDensityError)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Net Operating Income", fontSize = 10.sp, color = HighDensitySecondaryText)
                        Text(
                            text = "KSh ${String.format("%,d", netProfit)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (netProfit >= 0) HighDensityPrimary else HighDensityError
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = HighDensityBg),
                    border = BorderStroke(1.dp, HighDensityOutline),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("RECORD OPERATIONAL EXPENSE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText, letterSpacing = 1.sp)
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSel = expenseCategory == cat
                                Box(
                                    modifier = Modifier
                                        .background(if (isSel) HighDensityPrimary else Color.White, RoundedCornerShape(8.dp))
                                        .border(1.dp, if (isSel) HighDensityPrimary else HighDensityOutline, RoundedCornerShape(8.dp))
                                        .clickable { expenseCategory = cat }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else HighDensityText)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = expenseDesc,
                                onValueChange = { expenseDesc = it },
                                label = { Text("Expense Details", fontSize = 11.sp) },
                                modifier = Modifier.weight(1.5f),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HighDensityPrimary,
                                    unfocusedBorderColor = HighDensityOutline
                                )
                            )

                            OutlinedTextField(
                                value = expenseAmountStr,
                                onValueChange = { expenseAmountStr = it },
                                label = { Text("Amt (KSh)", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HighDensityPrimary,
                                    unfocusedBorderColor = HighDensityOutline
                                )
                            )

                            Button(
                                onClick = {
                                    val amt = expenseAmountStr.toIntOrNull() ?: 1000
                                    expenses.add(
                                        0,
                                        Expense(
                                            id = "EXP-${(904..999).random()}",
                                            date = "14 Jul 2026",
                                            category = expenseCategory,
                                            description = if (expenseDesc.isNotEmpty()) expenseDesc else "$expenseCategory Operations",
                                            amount = amt
                                        )
                                    )
                                    showSuccess = true
                                    expenseDesc = ""
                                    expenseAmountStr = ""
                                },
                                enabled = expenseAmountStr.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Record", fontSize = 11.sp)
                            }
                        }

                        if (showSuccess) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(HighDensitySuccess.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .border(1.dp, HighDensitySuccess, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = HighDensitySuccess, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Expense logged & ledger updated in real time!", color = HighDensitySuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            LaunchedEffect(showSuccess) {
                                kotlinx.coroutines.delay(3000)
                                showSuccess = false
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "OPERATING CASH DEBITS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensitySecondaryText,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    expenses.forEach { exp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, HighDensityOutline, RoundedCornerShape(10.dp))
                                .background(HighDensityBg)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(exp.category, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(HighDensityError.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Debit", color = HighDensityError, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(exp.description, fontSize = 11.sp, color = HighDensitySecondaryText, modifier = Modifier.padding(top = 2.dp))
                                Text("Date logged: ${exp.date} | Ref: ${exp.id}", fontSize = 10.sp, color = HighDensitySecondaryText)
                            }
                            Text("– KSh ${String.format("%,d", exp.amount)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HighDensityError)
                        }
                    }
                }
            }
        }
    }
}

// 15. Coming Soon Screen
@Composable
fun ComingSoonScreen(view: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(HighDensityPrimaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = HighDensityPrimary, modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "${view.replaceFirstChar { it.uppercase() }} Screen",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = HighDensityText,
            fontFamily = FontFamily.Serif
        )
        Text(
            text = "Feature under construction. Configure details for messaging alerts, financial reporting summaries, or advanced system metering values, and I will program it into this exact High Density landscape.",
            fontSize = 13.sp,
            color = HighDensitySecondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
            lineHeight = 18.sp
        )
    }
}

fun getPageTitle(view: String): String {
    return when (view) {
        "saas_manager" -> "SaaS Reseller & Landlord Hub"
        "dashboard" -> "Overview Dashboard"
        "tenant_portal" -> "Tenant Portal Dashboard"
        "messages" -> "Alert Notifications"
        "reports" -> "Performance Analytics"
        "sent" -> "Outbox Ledger"
        "tenants" -> "Active Leases"
        "houses" -> "Property Portfolios"
        "billing_table" -> "Billing spreadsheet"
        "rent" -> "Rent Billings"
        "water" -> "Water Billings"
        "meters" -> "Meter Inventory"
        "payment_dashboard" -> "Payment Analytics"
        "config" -> "Ledger Configuration"
        "deposits" -> "Tenancy Deposits Ledger"
        "received" -> "Payments Received"
        "allocations" -> "Awaiting Allocation"
        "account" -> "Consolidated Account"
        else -> "Portal Screen"
    }
}

// 16. Unit Management Dialog
@Composable
fun ManageHouseDialog(
    house: House,
    tenantsList: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (updatedHouse: House) -> Unit,
    onDelete: (houseId: String) -> Unit
) {
    var property by remember { mutableStateOf(house.property) }
    var unit by remember { mutableStateOf(house.unit) }
    var type by remember { mutableStateOf(house.type) }
    var rentText by remember { mutableStateOf(house.rent.toString()) }
    var status by remember { mutableStateOf(house.status) }
    var tenant by remember { mutableStateOf(house.tenant) }
    var maintenanceStatus by remember { mutableStateOf(house.maintenanceStatus) }
    var lastMaintenanceDate by remember { mutableStateOf(house.lastMaintenanceDate) }
    var meterNo by remember { mutableStateOf(house.meterNo) }
    var notes by remember { mutableStateOf(house.notes) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, HighDensityOutline)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Manage Unit ${house.unit}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                    IconButton(
                        onClick = { onDelete(house.id) }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Unit", tint = HighDensityError)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = property,
                    onValueChange = { property = it },
                    label = { Text("Property Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit Code") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Unit Type") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = rentText,
                    onValueChange = { rentText = it },
                    label = { Text("Monthly Rent") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Unit Occupancy Status", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Vacant", "Occupied", "Arrears").forEach { s ->
                        val isSel = status == s
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSel) HighDensityPrimary else HighDensitySurfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSel) HighDensityPrimary else HighDensityOutline,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { 
                                    status = s 
                                    if (s == "Vacant") {
                                        tenant = "—"
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = s,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else HighDensityText
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (status != "Vacant") {
                    Text("Assigned Tenant", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                    OutlinedTextField(
                        value = tenant,
                        onValueChange = { tenant = it },
                        label = { Text("Enter or select tenant name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Suggestions:", fontSize = 10.sp, color = HighDensitySecondaryText)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tenantsList.forEach { tName ->
                            Box(
                                modifier = Modifier
                                    .background(HighDensitySurfaceVariant, RoundedCornerShape(6.dp))
                                    .border(1.dp, HighDensityOutline, RoundedCornerShape(6.dp))
                                    .clickable { tenant = tName }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                  Text(tName, fontSize = 10.sp, color = HighDensityText)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text("Maintenance / Condition Level", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Operational", "Under Maintenance", "Repairs Scheduled", "Inspection Required").forEach { m ->
                        val isSel = maintenanceStatus == m
                        val activeColor = when (m) {
                            "Operational" -> HighDensitySuccess
                            "Under Maintenance" -> HighDensityError
                            else -> HighDensityWarning
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (isSel) activeColor else HighDensitySurfaceVariant,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSel) activeColor else HighDensityOutline,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { 
                                    maintenanceStatus = m 
                                    if (m == "Operational" && lastMaintenanceDate == "—") {
                                        lastMaintenanceDate = "14 Jul 2026"
                                    }
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = m.replace(" ", "\n"),
                                fontSize = 8.sp,
                                lineHeight = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else HighDensityText,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = meterNo,
                    onValueChange = { meterNo = it },
                    label = { Text("Water Meter Number (e.g. MTR-A3)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = lastMaintenanceDate,
                    onValueChange = { lastMaintenanceDate = it },
                    label = { Text("Last Maintenance Date") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Manager Desk Comments & Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = HighDensitySecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val rentVal = rentText.toIntOrNull() ?: house.rent
                            onConfirm(
                                house.copy(
                                    property = property,
                                    unit = unit,
                                    type = type,
                                    rent = rentVal,
                                    status = status,
                                    tenant = tenant,
                                    maintenanceStatus = maintenanceStatus,
                                    lastMaintenanceDate = lastMaintenanceDate,
                                    meterNo = meterNo,
                                    notes = notes
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                    ) {
                        Text("Save Changes", color = Color.White)
                    }
                }
            }
        }
    }
}

// 17. Consolidated Billing Table Component
@Composable
fun BillingTableView(
    rentBillings: SnapshotStateList<RentBilling>,
    waterBillings: SnapshotStateList<WaterBilling>,
    sentMessages: SnapshotStateList<SentMessage>,
    searchText: String,
    onRecordPayment: (tenantName: String) -> Unit,
    onMessageTenant: (tenantName: String) -> Unit
) {
    val filteredBillings = rentBillings.filter {
        it.tenant.contains(searchText, ignoreCase = true) || it.unit.contains(searchText, ignoreCase = true)
    }

    var expandedRowUnit by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().testTag("billing_table_view")) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensityPrimaryContainer),
            border = BorderStroke(1.dp, HighDensityPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Consolidated July 2026 Billing Register",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityPrimary
                )
                Text(
                    text = "Combines monthly base rent and metered water utility billing in a tabular spreadsheet format for easy landlord auditing.",
                    fontSize = 11.sp,
                    color = HighDensityPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Horizontal scroll container for the spreadsheet
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, HighDensityOutline),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
                Column {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .background(HighDensitySurfaceVariant)
                            .padding(vertical = 12.dp)
                    ) {
                        val headers = listOf(
                            "Unit" to 110,
                            "Tenant Name" to 140,
                            "Rent Due" to 110,
                            "Rent Paid" to 110,
                            "Water Vol" to 90,
                            "Water Bill" to 100,
                            "Total Due" to 110,
                            "Total Paid" to 110,
                            "Balance" to 110,
                            "Status" to 100,
                            "Actions" to 140
                        )
                        headers.forEach { (text, width) ->
                            Text(
                                text = text.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityPrimary,
                                modifier = Modifier.width(width.dp).padding(horizontal = 10.dp),
                                textAlign = if (text == "Unit" || text == "Tenant Name" || text == "Actions" || text == "Status") TextAlign.Start else TextAlign.End
                            )
                        }
                    }

                    HorizontalDivider(color = HighDensityOutline)

                    // Table Rows
                    if (filteredBillings.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp).width(1200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No billing rows match your search query", color = HighDensitySecondaryText, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.width(1220.dp)) {
                            itemsIndexed(filteredBillings) { index, rent ->
                                val rowBg = if (index % 2 == 0) Color.White else HighDensityLightBlue.copy(alpha = 0.5f)
                                
                                // Find corresponding water billing
                                val water = waterBillings.find { it.unit == rent.unit }
                                val waterUnits = water?.units ?: 0
                                val waterAmt = water?.amount ?: 0
                                val waterPaid = if (water?.status == "Paid") waterAmt else 0

                                val totalBilled = rent.billed + waterAmt
                                val totalPaid = rent.paid + waterPaid
                                val balance = maxOf(0, totalBilled - totalPaid)
                                
                                val status = when {
                                    balance == 0 -> "Fully Paid"
                                    totalPaid > 0 -> "Partial"
                                    else -> "Unpaid"
                                }

                                val isExpanded = expandedRowUnit == rent.unit
                                val rowBgColor = if (isExpanded) HighDensityPrimary.copy(alpha = 0.08f) else rowBg

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBgColor)
                                        .animateContentSize()
                                        .clickable {
                                            expandedRowUnit = if (isExpanded) null else rent.unit
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Unit
                                        Text(
                                            text = rent.unit.substringAfter(" – "),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityText,
                                            modifier = Modifier.width(110.dp).padding(horizontal = 10.dp)
                                        )
                                        // Tenant
                                        Text(
                                            text = rent.tenant,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = HighDensityText,
                                            modifier = Modifier.width(140.dp).padding(horizontal = 10.dp)
                                        )
                                        // Rent Due
                                        Text(
                                            text = "KSh ${String.format("%,d", rent.billed)}",
                                            fontSize = 12.sp,
                                            color = HighDensityText,
                                            modifier = Modifier.width(110.dp).padding(horizontal = 10.dp),
                                            textAlign = TextAlign.End
                                        )
                                        // Rent Paid
                                        Text(
                                            text = "KSh ${String.format("%,d", rent.paid)}",
                                            fontSize = 12.sp,
                                            color = HighDensitySuccess,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(110.dp).padding(horizontal = 10.dp),
                                            textAlign = TextAlign.End
                                        )
                                        // Water Volume
                                        Text(
                                            text = "$waterUnits m³",
                                            fontSize = 12.sp,
                                            color = HighDensityText,
                                            modifier = Modifier.width(90.dp).padding(horizontal = 10.dp),
                                            textAlign = TextAlign.End
                                        )
                                        // Water Bill
                                        Text(
                                            text = "KSh ${String.format("%,d", waterAmt)}",
                                            fontSize = 12.sp,
                                            color = HighDensityText,
                                            modifier = Modifier.width(100.dp).padding(horizontal = 10.dp),
                                            textAlign = TextAlign.End
                                        )
                                        // Total Billed
                                        Text(
                                            text = "KSh ${String.format("%,d", totalBilled)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityText,
                                            modifier = Modifier.width(110.dp).padding(horizontal = 10.dp),
                                            textAlign = TextAlign.End
                                        )
                                        // Total Paid
                                        Text(
                                            text = "KSh ${String.format("%,d", totalPaid)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensitySuccess,
                                            modifier = Modifier.width(110.dp).padding(horizontal = 10.dp),
                                            textAlign = TextAlign.End
                                        )
                                        // Balance
                                        Text(
                                            text = "KSh ${String.format("%,d", balance)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (balance > 0) HighDensityError else HighDensitySuccess,
                                            modifier = Modifier.width(110.dp).padding(horizontal = 10.dp),
                                            textAlign = TextAlign.End
                                        )
                                        // Status Badge
                                        Box(
                                            modifier = Modifier.width(100.dp).padding(horizontal = 10.dp)
                                        ) {
                                            val (color, bg) = when (status) {
                                                "Fully Paid" -> HighDensitySuccess to HighDensitySuccess.copy(alpha = 0.15f)
                                                "Partial" -> HighDensitySecondary to HighDensitySecondary.copy(alpha = 0.15f)
                                                else -> HighDensityError to HighDensityError.copy(alpha = 0.15f)
                                            }
                                            Text(
                                                text = status,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = color,
                                                modifier = Modifier
                                                    .background(bg, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        // Actions
                                        Row(
                                            modifier = Modifier.width(140.dp).padding(horizontal = 10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            IconButton(
                                                onClick = { onRecordPayment(rent.tenant) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Payment,
                                                    contentDescription = "Pay",
                                                    tint = HighDensityPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onMessageTenant(rent.tenant) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Message,
                                                    contentDescription = "Message",
                                                    tint = HighDensitySecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Interactive drawer on expansion
                                    if (isExpanded) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(HighDensitySurfaceVariant.copy(alpha = 0.5f))
                                                .border(BorderStroke(1.dp, HighDensityOutline))
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Formula algebra
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "📊 UNIT INVOICE RECONCILIATION ALGEBRA",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = HighDensityPrimary,
                                                        letterSpacing = 1.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Base Lease Rent: KSh ${String.format("%,d", rent.billed)}\n💧 Metered Water (${waterUnits} m³ @ KSh 120): KSh ${String.format("%,d", waterAmt)}\nTotal Unit Billings: KSh ${String.format("%,d", totalBilled)}\nTotal Payments Credited: KSh ${String.format("%,d", totalPaid)}\nPending Ledger Balance: KSh ${String.format("%,d", balance)}",
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = HighDensityText,
                                                        lineHeight = 16.sp
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(16.dp))

                                                // Live actions
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Add late penalty (+KSh 500)
                                                    Button(
                                                        onClick = {
                                                            val index = rentBillings.indexOfFirst { it.unit == rent.unit && it.month == rent.month }
                                                            if (index != -1) {
                                                                val item = rentBillings[index]
                                                                rentBillings[index] = item.copy(billed = item.billed + 500, balance = item.balance + 500)
                                                            }
                                                            sentMessages.add(
                                                                0,
                                                                SentMessage(
                                                                    id = "PEN-${(100..999).random()}",
                                                                    date = "14 Jul 2026",
                                                                    recipients = "${rent.tenant} (${rent.unit.split(" – ").last()})",
                                                                    messageText = "Dear ${rent.tenant}, a late rent penalty of KSh 500 has been applied to your July lease. New outstanding balance is KSh ${balance + 500}.",
                                                                    status = "Emailed & Delivered",
                                                                    category = "Rent Reminder"
                                                                )
                                                            )
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityError),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                        modifier = Modifier.height(30.dp)
                                                    ) {
                                                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Penalty +KSh 500", fontSize = 10.sp, color = Color.White)
                                                    }

                                                    // Clear balance
                                                    if (balance > 0) {
                                                        Button(
                                                            onClick = {
                                                                // Clear water
                                                                if (water != null && water.status == "Unpaid") {
                                                                    val wIndex = waterBillings.indexOfFirst { it.unit == water.unit }
                                                                    if (wIndex != -1) {
                                                                        waterBillings[wIndex] = waterBillings[wIndex].copy(status = "Paid")
                                                                    }
                                                                }
                                                                // Clear rent
                                                                val rIndex = rentBillings.indexOfFirst { it.unit == rent.unit && it.month == rent.month }
                                                                if (rIndex != -1) {
                                                                    val item = rentBillings[rIndex]
                                                                    rentBillings[rIndex] = item.copy(
                                                                        paid = item.billed,
                                                                        balance = 0
                                                                    )
                                                                }
                                                                sentMessages.add(
                                                                    0,
                                                                    SentMessage(
                                                                        id = "REC-${(100..999).random()}",
                                                                        date = "14 Jul 2026",
                                                                        recipients = "${rent.tenant} (${rent.unit.split(" – ").last()})",
                                                                        messageText = "Dear ${rent.tenant}, we have received your payment of KSh $balance. Your account is now fully paid. Thank you!",
                                                                        status = "Emailed & Delivered",
                                                                        category = "Rent Reminder"
                                                                    )
                                                                )
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = HighDensitySuccess),
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                            modifier = Modifier.height(30.dp)
                                                        ) {
                                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Instant Clear", fontSize = 10.sp, color = Color.White)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(color = HighDensityOutline.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// 18. Payment Dashboard View Component
@Composable
fun PaymentDashboardView(
    paymentsReceived: List<PaymentReceived>,
    allocations: List<Allocation>,
    searchText: String,
    onAddPaymentClick: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val filteredPayments = paymentsReceived.filter {
        it.tenant.contains(searchText, ignoreCase = true) || it.ref.contains(searchText, ignoreCase = true)
    }

    val totalRentPaid = paymentsReceived.filter { it.allocated }.sumOf { it.amount }
    val totalUnallocated = paymentsReceived.filter { !it.allocated }.sumOf { it.amount }
    val mpesaAmt = paymentsReceived.filter { it.method == "M-Pesa" }.sumOf { it.amount }
    val bankAmt = paymentsReceived.filter { it.method == "Bank" }.sumOf { it.amount }
    val cashAmt = paymentsReceived.filter { it.method == "Cash" }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("payment_dashboard_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Cards
        item {
            Column {
                Text(
                    text = "Financial Collections Overview",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensitySecondaryText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardStatCard(
                            label = "Allocated Funds",
                            value = "KSh ${String.format("%,d", totalRentPaid)}",
                            subText = "Cleared Rent/Utilities",
                            accentColor = HighDensitySuccess
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardStatCard(
                            label = "Unallocated Cash",
                            value = "KSh ${String.format("%,d", totalUnallocated)}",
                            subText = "Suspense Ledger",
                            accentColor = HighDensityWarning
                        )
                    }
                }
            }
        }

        // Channel Distribution Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, HighDensityOutline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Collection Methods Breakdown", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val total = (mpesaAmt + bankAmt + cashAmt).toFloat().coerceAtLeast(1f)
                        val mpesaPct = mpesaAmt / total
                        val bankPct = bankAmt / total
                        val cashPct = cashAmt / total

                        // Visual progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(HighDensityOutline)
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                if (mpesaPct > 0) {
                                    Box(modifier = Modifier.fillMaxHeight().weight(mpesaPct).background(HighDensityPrimary))
                                }
                                if (bankPct > 0) {
                                    Box(modifier = Modifier.fillMaxHeight().weight(bankPct).background(HighDensitySecondary))
                                }
                                if (cashPct > 0) {
                                    Box(modifier = Modifier.fillMaxHeight().weight(cashPct).background(HighDensitySuccess))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(HighDensityPrimary, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("M-Pesa: KSh ${String.format("%,d", mpesaAmt)}", fontSize = 12.sp, color = HighDensityText)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(HighDensitySecondary, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bank: KSh ${String.format("%,d", bankAmt)}", fontSize = 12.sp, color = HighDensityText)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(HighDensitySuccess, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cash: KSh ${String.format("%,d", cashAmt)}", fontSize = 12.sp, color = HighDensityText)
                        }
                    }
                }
            }
        }

        // Recent Payments
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Receipt Log & Allocations",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HighDensitySecondaryText
                )
                Button(
                    onClick = onAddPaymentClick,
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Record Payment", fontSize = 12.sp)
                }
            }
        }

        if (filteredPayments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions match search criteria", color = HighDensitySecondaryText, fontSize = 13.sp)
                }
            }
        } else {
            items(filteredPayments) { p ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (p.method == "M-Pesa") Icons.Default.Phone else Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = HighDensityPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = p.tenant, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HighDensityText)
                            }
                            Text(
                                text = if (p.allocated) "Allocated" else "Suspense",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (p.allocated) HighDensitySuccess else HighDensityWarning,
                                modifier = Modifier
                                    .background(
                                        color = if (p.allocated) HighDensitySuccess.copy(alpha = 0.15f) else HighDensityWarning.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Ref: ${p.ref} (${p.method})", fontSize = 12.sp, color = HighDensitySecondaryText)
                            Text(text = "KSh ${p.amount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Date: ${p.date}", fontSize = 11.sp, color = HighDensitySecondaryText)
                            if (!p.allocated) {
                                TextButton(
                                    onClick = { onNavigate("allocations") },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Resolve suspense", fontSize = 11.sp, color = HighDensitySecondary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 18. Simulated Tenant Portal View
@Composable
fun TenantPortalView(
    tenants: List<Tenant>,
    rentBillings: SnapshotStateList<RentBilling>,
    waterBillings: SnapshotStateList<WaterBilling>,
    meterReplacements: SnapshotStateList<MeterReplacement>,
    sentMessages: SnapshotStateList<SentMessage>,
    paymentsReceived: SnapshotStateList<PaymentReceived>,
    allocations: SnapshotStateList<Allocation>,
    onNavigate: (String) -> Unit,
    clientRequests: SnapshotStateList<ClientRequest>
) {
    var selectedTenantName by remember { mutableStateOf(tenants.firstOrNull()?.name ?: "") }
    val selectedTenant = tenants.find { it.name == selectedTenantName }
    
    // Local state for self water meter reading input
    var userMeterReadingStr by remember(selectedTenantName) { mutableStateOf("") }
    
    // Local state for payment simulation
    var simulationPaymentAmtStr by remember(selectedTenantName) { mutableStateOf("") }
    var paymentSimulationSuccessMsg by remember { mutableStateOf<String?>(null) }
    
    // Local state for export feedback
    var exportFeedbackMsg by remember { mutableStateOf<String?>(null) }
    
    // Local state for support request ticket
    var supportTicketText by remember { mutableStateOf("") }
    var supportTicketSuccessMsg by remember { mutableStateOf<String?>(null) }

    // Look up current rent billing and water billing
    val rentBilling = rentBillings.find { it.tenant == selectedTenantName }
    val waterBilling = waterBillings.find { it.unit == selectedTenant?.unit }
    
    val rentBilled = rentBilling?.billed ?: 0
    val rentPaid = rentBilling?.paid ?: 0
    val rentBalance = maxOf(0, rentBilled - rentPaid)
    
    val waterUnits = waterBilling?.units ?: 0
    val waterAmt = waterBilling?.amount ?: 0
    val waterPaid = if (waterBilling?.status == "Paid") waterAmt else 0
    val waterBalance = maxOf(0, waterAmt - waterPaid)
    
    val totalOutstanding = rentBalance + waterBalance

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tenant_portal_view")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Selector segment for landlord to pick tenant context
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = HighDensityPrimaryContainer),
                border = BorderStroke(1.dp, HighDensityPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Active Tenant Portal Context Simulator",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityPrimary
                    )
                    Text(
                        text = "Click a tenant below to view and test their personalized digital tenant portal hub in real-time.",
                        fontSize = 11.sp,
                        color = HighDensityPrimary.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Horizontal scrollable selector of tenant pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tenants.forEach { tenant ->
                            val isSelected = tenant.name == selectedTenantName
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) HighDensityPrimary else Color.White,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .border(
                                        BorderStroke(1.dp, HighDensityPrimary),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        selectedTenantName = tenant.name
                                        paymentSimulationSuccessMsg = null
                                        exportFeedbackMsg = null
                                        supportTicketSuccessMsg = null
                                        supportTicketText = ""
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tenant.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else HighDensityPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedTenant != null) {
            // 2. Beautiful branded header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(HighDensityPrimary, HighDensityPrimary.copy(alpha = 0.85f))
                            )
                        )
                        .border(BorderStroke(2.dp, Color(0xFFD4AF37)), RoundedCornerShape(12.dp)) // Gold border
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "JEBLORD AGENCY GROUP",
                                color = Color(0xFFD4AF37), // Gold
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SECURE PORTAL",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Welcome, ${selectedTenant.name}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Unit Allocation: ${selectedTenant.unit}  •  Registered Phone: ${selectedTenant.phone}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Toast feedback alerts
            if (paymentSimulationSuccessMsg != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = HighDensitySuccess.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, HighDensitySuccess),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HighDensitySuccess)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(paymentSimulationSuccessMsg ?: "", color = HighDensityText, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (exportFeedbackMsg != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = HighDensityPrimaryContainer),
                        border = BorderStroke(1.dp, HighDensityPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = HighDensityPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(exportFeedbackMsg ?: "", color = HighDensityText, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (supportTicketSuccessMsg != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = HighDensitySecondaryContainer),
                        border = BorderStroke(1.dp, HighDensitySecondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Message, contentDescription = null, tint = HighDensitySecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(supportTicketSuccessMsg ?: "", color = HighDensityText, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 3. Current balance visualization
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, HighDensityOutline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("OUTSTANDING BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "KSh ${String.format("%,d", totalOutstanding)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalOutstanding > 0) HighDensityError else HighDensitySuccess,
                                fontFamily = FontFamily.Serif
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (totalOutstanding > 0) "Arrears status: ${selectedTenant.status}" else "Account is fully paid!",
                                fontSize = 10.sp,
                                color = if (totalOutstanding > 0) HighDensityError else HighDensitySuccess
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, HighDensityOutline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("LAST RECORDED READING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${waterBilling?.currReading ?: 0} m³",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityPrimary,
                                fontFamily = FontFamily.Serif
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Consumed: $waterUnits m³ (KSh ${String.format("%,d", waterAmt)})",
                                fontSize = 10.sp,
                                color = HighDensitySecondaryText
                            )
                        }
                    }
                }
            }

            // 4. Submit Water Meter Self-Reading Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💧 Water Meter Self-Reading Submission",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityText
                        )
                        Text(
                            text = "Avoid estimated bills. Submit your current meter dial units directly to the database.",
                            fontSize = 11.sp,
                            color = HighDensitySecondaryText,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        val prevVal = waterBilling?.currReading ?: 0
                        
                        OutlinedTextField(
                            value = userMeterReadingStr,
                            onValueChange = { userMeterReadingStr = it },
                            label = { Text("New Meter Reading (m³)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HighDensityPrimary,
                                unfocusedBorderColor = HighDensityOutline
                            )
                        )

                        val inputReading = userMeterReadingStr.toIntOrNull()
                        if (inputReading != null && inputReading >= prevVal) {
                            val computedUnits = inputReading - prevVal
                            val estimatedCost = computedUnits * 120
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(HighDensityLightBlue.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(text = "Provisional Units Consumed: $computedUnits m³", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                                Text(text = "Estimated Utility Levy (@ KSh 120/m³): KSh ${String.format("%,d", estimatedCost)}", fontSize = 11.sp, color = HighDensityText)
                                
                                if (computedUnits > 10) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(HighDensityError.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "⚠️ LEAK DETECTION ALERTER: Water consumption exceeds 10m³ trigger! Possible fixture, pipe, or cistern leak. Please verify plumbing.",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityError
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val input = userMeterReadingStr.toIntOrNull()
                                if (input != null && input >= prevVal) {
                                    val computedUnits = input - prevVal
                                    val estimatedCost = computedUnits * 120
                                    
                                    // Update water billings
                                    val index = waterBillings.indexOfFirst { it.unit == selectedTenant.unit }
                                    if (index != -1) {
                                        waterBillings[index] = WaterBilling(
                                            unit = selectedTenant.unit,
                                            prevReading = prevVal,
                                            currReading = input,
                                            units = computedUnits,
                                            rate = 120,
                                            amount = estimatedCost,
                                            status = "Unpaid"
                                        )
                                    } else {
                                        waterBillings.add(
                                            0,
                                            WaterBilling(
                                                unit = selectedTenant.unit,
                                                prevReading = prevVal,
                                                currReading = input,
                                                units = computedUnits,
                                                rate = 120,
                                                amount = estimatedCost,
                                                status = "Unpaid"
                                            )
                                        )
                                    }
                                    
                                    // Append to sentMessages
                                    sentMessages.add(
                                        0,
                                        SentMessage(
                                            id = "MTR-${(100..999).random()}",
                                            date = "14 Jul 2026",
                                            recipients = "Landlord Audit Desk",
                                            messageText = "Tenant ${selectedTenant.name} (${selectedTenant.unit}) submitted meter self-reading: $input m³ (Consumption: $computedUnits m³, Estimated Water Charge: KSh $estimatedCost).",
                                            status = "Emailed & Saved",
                                            category = "Announcement"
                                        )
                                    )
                                    
                                    userMeterReadingStr = ""
                                    paymentSimulationSuccessMsg = "Water meter reading logged successfully. A confirmation message has been dispatched."
                                }
                            },
                            enabled = inputReading != null && inputReading >= prevVal,
                            colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Submit Official Reading", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }

            // 5. Lipa Na M-Pesa Simulator
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💸 Live Lipa Na M-Pesa API Simulator",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityText
                        )
                        Text(
                            text = "Test instant payment routing. Simulates customer paying their ledger balance.",
                            fontSize = 11.sp,
                            color = HighDensitySecondaryText,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = simulationPaymentAmtStr,
                            onValueChange = { simulationPaymentAmtStr = it },
                            label = { Text("Payment Amount (KSh)") },
                            placeholder = { Text("Outstanding: KSh $totalOutstanding") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HighDensityPrimary,
                                unfocusedBorderColor = HighDensityOutline
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val payAmt = simulationPaymentAmtStr.toIntOrNull()
                                if (payAmt != null && payAmt > 0) {
                                    val randomCode = "SGK" + (100000..999999).random() + "Y"
                                    
                                    // 1. Append to paymentsReceived
                                    paymentsReceived.add(
                                        0,
                                        PaymentReceived(
                                            date = "14 Jul 2026",
                                            ref = randomCode,
                                            tenant = selectedTenant.name,
                                            method = "M-Pesa",
                                            amount = payAmt,
                                            allocated = true
                                        )
                                    )
                                    
                                    // 2. Append to allocations
                                    allocations.add(
                                        0,
                                        Allocation(
                                            ref = randomCode,
                                            amount = payAmt,
                                            receivedOn = "14 Jul 2026",
                                            suggestedUnit = selectedTenant.unit,
                                            note = "Self-portal payment"
                                        )
                                    )

                                    // 3. Mutate rent balances and water balances
                                    var remaining = payAmt
                                    
                                    // Pay water first
                                    if (waterBalance > 0 && waterBilling != null) {
                                        val appliedToWater = minOf(remaining, waterBalance)
                                        val waterIndex = waterBillings.indexOfFirst { it.unit == selectedTenant.unit }
                                        if (waterIndex != -1) {
                                            val isFullyPaid = appliedToWater == waterBalance
                                            waterBillings[waterIndex] = waterBillings[waterIndex].copy(
                                                status = if (isFullyPaid) "Paid" else "Unpaid"
                                            )
                                        }
                                        remaining -= appliedToWater
                                    }
                                    
                                    // Pay rent next
                                    if (remaining > 0 && rentBalance > 0 && rentBilling != null) {
                                        val appliedToRent = minOf(remaining, rentBalance)
                                        val rentIndex = rentBillings.indexOfFirst { it.tenant == selectedTenant.name }
                                        if (rentIndex != -1) {
                                            val currentItem = rentBillings[rentIndex]
                                            rentBillings[rentIndex] = currentItem.copy(
                                                paid = currentItem.paid + appliedToRent,
                                                balance = maxOf(0, currentItem.balance - appliedToRent)
                                            )
                                        }
                                    }
                                    
                                    // 4. Send automated confirmation
                                    sentMessages.add(
                                        0,
                                        SentMessage(
                                            id = "TX-${(100..999).random()}",
                                            date = "14 Jul 2026",
                                            recipients = selectedTenant.name,
                                            messageText = "Dear ${selectedTenant.name}, your payment of KSh $payAmt via M-Pesa ref $randomCode was received and applied to unit ${selectedTenant.unit.substringAfter(" – ")}. Thank you!",
                                            status = "Emailed & SMS Dispatched",
                                            category = "Rent Reminder"
                                        )
                                    )
                                    
                                    simulationPaymentAmtStr = ""
                                    paymentSimulationSuccessMsg = "Payment of KSh $payAmt successfully processed via Lipa Na M-Pesa. Ref: $randomCode. Ledger balances auto-adjusted."
                                }
                            },
                            enabled = simulationPaymentAmtStr.toIntOrNull() != null && (simulationPaymentAmtStr.toIntOrNull() ?: 0) > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = HighDensitySecondary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simulate Lipa na M-Pesa Payment", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }

            // 6. Interactive Export & Statement section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📄 Ledger Statement & Export Desk",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityText
                            )
                            Button(
                                onClick = {
                                    exportFeedbackMsg = "Exporting July Statement for ${selectedTenant.name} as CSV and PDF... Saved to Downloads folder! Total ledger liability exported: KSh $totalOutstanding."
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Simulate Export", fontSize = 10.sp, color = Color.White)
                            }
                        }
                        
                        Text(
                            text = "Official record of rent billing, utility tallies, and credits for unit ${selectedTenant.unit}.",
                            fontSize = 11.sp,
                            color = HighDensitySecondaryText,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(BorderStroke(1.dp, HighDensityOutline), RoundedCornerShape(8.dp))
                        ) {
                            // Header row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(HighDensitySurfaceVariant)
                                    .padding(8.dp)
                            ) {
                                Text("Billing Item", modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Billed", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                Text("Paid", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                Text("Balance", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                            }
                            
                            // Base Rent Row
                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                Text("Base Lease Rent (July)", modifier = Modifier.weight(1.5f), fontSize = 11.sp)
                                Text("KSh ${String.format("%,d", rentBilled)}", modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                                Text("KSh ${String.format("%,d", rentPaid)}", modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                                Text("KSh ${String.format("%,d", rentBalance)}", modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End, color = if (rentBalance > 0) HighDensityError else HighDensitySuccess, fontWeight = FontWeight.Bold)
                            }
                            
                            HorizontalDivider(color = HighDensityOutline.copy(alpha = 0.5f))
                            
                            // Water Utility Row
                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                Text("Metered Water ($waterUnits m³)", modifier = Modifier.weight(1.5f), fontSize = 11.sp)
                                Text("KSh ${String.format("%,d", waterAmt)}", modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                                Text("KSh ${String.format("%,d", waterPaid)}", modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End)
                                Text("KSh ${String.format("%,d", waterBalance)}", modifier = Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.End, color = if (waterBalance > 0) HighDensityError else HighDensitySuccess, fontWeight = FontWeight.Bold)
                            }
                            
                            HorizontalDivider(color = HighDensityOutline)
                            
                            // Total Row
                            Row(modifier = Modifier.fillMaxWidth().background(HighDensityLightBlue.copy(alpha = 0.2f)).padding(8.dp)) {
                                Text("Consolidated Total", modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("KSh ${String.format("%,d", rentBilled + waterAmt)}", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                Text("KSh ${String.format("%,d", rentPaid + waterPaid)}", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                Text("KSh ${String.format("%,d", totalOutstanding)}", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = if (totalOutstanding > 0) HighDensityError else HighDensitySuccess)
                            }
                        }
                    }
                }
            }

            // 7. Client Requests Services Hub (Repairs, Painting, Comments)
            item {
                var selectedRequestType by remember { mutableStateOf("Repairs") }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🛠️ Client Request & Maintenance Hub",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityText
                        )
                        Text(
                            text = "Request repairs, painting, or leave a feedback comment for management instantly.",
                            fontSize = 11.sp,
                            color = HighDensitySecondaryText,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        // Selector of Request Type
                        Text(
                            text = "Request Category:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensitySecondaryText,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Repairs", "Painting", "Comment").forEach { type ->
                                val isSelected = selectedRequestType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) HighDensitySecondaryContainer else HighDensityBg, RoundedCornerShape(6.dp))
                                        .border(1.dp, if (isSelected) HighDensityPrimary else HighDensityOutline, RoundedCornerShape(6.dp))
                                        .clickable { selectedRequestType = type }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        val icon = when (type) {
                                            "Repairs" -> Icons.Default.Build
                                            "Painting" -> Icons.Default.Brush
                                            else -> Icons.Default.Comment
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) HighDensityPrimary else HighDensitySecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = type,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) HighDensityPrimary else HighDensitySecondary
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = supportTicketText,
                            onValueChange = { supportTicketText = it },
                            label = { Text("Describe what you need in detail (e.g., room location, issues)...") },
                            modifier = Modifier.fillMaxWidth().height(90.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = HighDensityPrimary,
                                unfocusedBorderColor = HighDensityOutline
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (supportTicketText.isNotEmpty()) {
                                    val randomId = "REQ-" + (100..999).random()
                                    clientRequests.add(
                                        0,
                                        ClientRequest(
                                            id = randomId,
                                            tenantName = selectedTenant.name,
                                            unit = selectedTenant.unit,
                                            requestType = selectedRequestType,
                                            description = supportTicketText,
                                            requestDate = "15 Jul 2026",
                                            status = "Pending"
                                        )
                                    )
                                    sentMessages.add(
                                        0,
                                        SentMessage(
                                            id = "MSG-${(100..999).random()}",
                                            date = "15 Jul 2026",
                                            recipients = "Management Desk",
                                            messageText = "Tenant ${selectedTenant.name} (${selectedTenant.unit}) requested $selectedRequestType: $supportTicketText",
                                            status = "Delivered",
                                            category = "Announcement"
                                        )
                                    )
                                    supportTicketText = ""
                                    supportTicketSuccessMsg = "Your $selectedRequestType request (ID: $randomId) has been filed successfully! The property owner has been notified."
                                }
                            },
                            enabled = supportTicketText.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Submit Service Request", fontSize = 12.sp, color = Color.White)
                        }

                        // Success Alert
                        if (supportTicketSuccessMsg != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = HighDensitySuccess.copy(alpha = 0.12f)),
                                border = BorderStroke(1.dp, HighDensitySuccess)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = HighDensitySuccess, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(supportTicketSuccessMsg ?: "", fontSize = 11.sp, color = HighDensityText)
                                }
                            }
                        }

                        // Request history for this specific tenant
                        val myRequests = clientRequests.filter { it.tenantName == selectedTenant.name }
                        if (myRequests.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = HighDensityOutline, modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "Your Request Log history",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityText,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                myRequests.forEach { req ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(HighDensityBg, RoundedCornerShape(8.dp))
                                            .border(1.dp, HighDensityOutline, RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    val typeColor = when (req.requestType) {
                                                        "Repairs" -> HighDensityError
                                                        "Painting" -> HighDensityPrimary
                                                        else -> HighDensitySecondary
                                                    }
                                                    Text(
                                                        text = req.requestType,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = typeColor
                                                    )
                                                    Text("• ${req.id}", fontSize = 9.sp, color = HighDensitySecondaryText)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            when (req.status) {
                                                                "Completed" -> HighDensitySuccess.copy(alpha = 0.15f)
                                                                "Scheduled" -> HighDensityWarning.copy(alpha = 0.15f)
                                                                else -> HighDensityError.copy(alpha = 0.15f)
                                                            },
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = req.status,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = when (req.status) {
                                                            "Completed" -> HighDensitySuccess
                                                            "Scheduled" -> HighDensityWarning
                                                            else -> HighDensityError
                                                        }
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = req.description, fontSize = 11.sp, color = HighDensityText)
                                            if (req.resolutionNotes.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Management log: ${req.resolutionNotes}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = HighDensityPrimary
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
        }
    }
}

@Composable
fun DepositsView(
    deposits: SnapshotStateList<DepositEntry>,
    depositTransactions: SnapshotStateList<DepositTransaction>,
    tenants: List<Tenant>,
    houses: List<House>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") }
    
    // Form action toggler: "Log Deposit", "Deduct Claim", "Issue Refund"
    var activeFormType by remember { mutableStateOf("None") } // "None", "Log", "Claim", "Refund"
    
    // State variables for "Log Deposit" Form
    var logTenantName by remember { mutableStateOf("") }
    var logAmountStr by remember { mutableStateOf("") }
    var logDateStr by remember { mutableStateOf("14 Jul 2026") }
    var logNotes by remember { mutableStateOf("") }
    var logSuccessMsg by remember { mutableStateOf<String?>(null) }
    
    // State variables for "Deduct Claim" Form
    var claimTenantName by remember { mutableStateOf("") }
    var claimAmountStr by remember { mutableStateOf("") }
    var claimReason by remember { mutableStateOf("") }
    var claimDateStr by remember { mutableStateOf("14 Jul 2026") }
    var claimSuccessMsg by remember { mutableStateOf<String?>(null) }
    
    // State variables for "Issue Refund" Form
    var refundTenantName by remember { mutableStateOf("") }
    var refundAmountStr by remember { mutableStateOf("") }
    var refundNotes by remember { mutableStateOf("") }
    var refundDateStr by remember { mutableStateOf("14 Jul 2026") }
    var refundSuccessMsg by remember { mutableStateOf<String?>(null) }

    // Computations
    val totalDepositsHeld = deposits.filter { it.status == "Held in Trust" || it.status == "Partially Refunded" }
        .sumOf { it.amountDeposited - it.amountRefunded - it.amountClaimed }
        
    val totalClaimsDeducted = deposits.sumOf { it.amountClaimed }
    val totalRefundsDisbursed = deposits.sumOf { it.amountRefunded }

    // Filtered list
    val filteredDeposits = deposits.filter { entry ->
        val matchesSearch = entry.tenantName.contains(searchQuery, ignoreCase = true) || entry.unit.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (statusFilter) {
            "All" -> true
            "Held in Trust" -> entry.status == "Held in Trust"
            "Partially Refunded" -> entry.status == "Partially Refunded"
            "Fully Refunded" -> entry.status == "Fully Refunded"
            else -> true
        }
        matchesSearch && matchesFilter
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("deposits_view_container")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top statistics cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Held in Trust
                Card(
                    modifier = Modifier.weight(1.2f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(HighDensityPrimaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = HighDensityPrimary, modifier = Modifier.size(12.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("HELD IN TRUST", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "KSh ${String.format("%,d", totalDepositsHeld)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityPrimary,
                            fontFamily = FontFamily.Serif
                        )
                        Text("Active secure escrow", fontSize = 9.sp, color = HighDensitySecondaryText)
                    }
                }

                // Claims Deducted
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFFFEE2E2), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Handyman, contentDescription = null, tint = HighDensityError, modifier = Modifier.size(12.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CLAIMS MADE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "KSh ${String.format("%,d", totalClaimsDeducted)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityError,
                            fontFamily = FontFamily.Serif
                        )
                        Text("For cleaning & repairs", fontSize = 9.sp, color = HighDensitySecondaryText)
                    }
                }

                // Refunds Issued
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(HighDensitySecondaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, tint = HighDensitySecondary, modifier = Modifier.size(12.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("REFUNDED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "KSh ${String.format("%,d", totalRefundsDisbursed)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensitySecondary,
                            fontFamily = FontFamily.Serif
                        )
                        Text("Disbursed to tenants", fontSize = 9.sp, color = HighDensitySecondaryText)
                    }
                }
            }
        }

        // Action selector panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = HighDensitySurfaceVariant),
                border = BorderStroke(1.dp, HighDensityOutline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🛠️ Escrow Deposit Management Actions",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityText
                    )
                    Text(
                        text = "Log new cash inputs, record repair claims, or process final refunds below.",
                        fontSize = 11.sp,
                        color = HighDensitySecondaryText,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // Row of Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Log" to "Log Deposit",
                            "Claim" to "Deduct Claim",
                            "Refund" to "Issue Refund"
                        ).forEach { (type, label) ->
                            val isSel = activeFormType == type
                            val containerCol = if (isSel) HighDensityPrimary else Color.White
                            val textCol = if (isSel) Color.White else HighDensityPrimary
                            val borderCol = if (isSel) HighDensityPrimary else HighDensityOutline

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(containerCol, RoundedCornerShape(8.dp))
                                    .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                                    .clickable {
                                        activeFormType = if (isSel) "None" else type
                                        // clear messages
                                        logSuccessMsg = null
                                        claimSuccessMsg = null
                                        refundSuccessMsg = null
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textCol
                                )
                            }
                        }
                    }

                    // Render selected form
                    when (activeFormType) {
                        "Log" -> {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = HighDensityOutline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text("LOG NEW SECURITY DEPOSIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Tenant pills selector
                            Text("Select Tenant", fontSize = 10.sp, color = HighDensitySecondaryText)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                tenants.forEach { t ->
                                    val isSel = logTenantName == t.name
                                    Box(
                                        modifier = Modifier
                                            .background(if (isSel) HighDensityPrimary else Color.White, RoundedCornerShape(20.dp))
                                            .border(1.dp, if (isSel) HighDensityPrimary else HighDensityOutline, RoundedCornerShape(20.dp))
                                            .clickable {
                                                logTenantName = t.name
                                                // auto-fill rent as recommended deposit size!
                                                logAmountStr = t.rent.toString()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(t.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else HighDensityText)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = logAmountStr,
                                onValueChange = { logAmountStr = it },
                                label = { Text("Deposit Amount (KSh)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = HighDensityPrimary,
                                    unfocusedBorderColor = HighDensityOutline
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = logNotes,
                                onValueChange = { logNotes = it },
                                label = { Text("Internal Note / Desk Comment") },
                                placeholder = { Text("e.g. Standard 1-month base deposit") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = HighDensityPrimary,
                                    unfocusedBorderColor = HighDensityOutline
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val amt = logAmountStr.toIntOrNull()
                                    val associatedTenant = tenants.find { it.name == logTenantName }
                                    if (amt != null && amt > 0 && associatedTenant != null) {
                                        val entry = DepositEntry(
                                            tenantName = logTenantName,
                                            unit = associatedTenant.unit,
                                            amountDeposited = amt,
                                            dateDeposited = logDateStr,
                                            status = "Held in Trust",
                                            notes = logNotes.ifEmpty { "Cash intake recorded" }
                                        )
                                        // Add or update
                                        val existingIndex = deposits.indexOfFirst { it.tenantName == logTenantName }
                                        if (existingIndex != -1) {
                                            deposits[existingIndex] = entry
                                        } else {
                                            deposits.add(0, entry)
                                        }

                                        val randomId = "TXD-${(107..999).random()}"
                                        depositTransactions.add(
                                            0,
                                            DepositTransaction(
                                                id = randomId,
                                                tenantName = logTenantName,
                                                date = logDateStr,
                                                type = "Deposit Received",
                                                amount = amt,
                                                reason = logNotes.ifEmpty { "Security deposit recorded safely in escrow." }
                                            )
                                        )

                                        logSuccessMsg = "Successfully recorded deposit of KSh $amt for $logTenantName."
                                        logTenantName = ""
                                        logAmountStr = ""
                                        logNotes = ""
                                    }
                                },
                                enabled = logTenantName.isNotEmpty() && logAmountStr.toIntOrNull() != null && (logAmountStr.toIntOrNull() ?: 0) > 0,
                                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Register Security Deposit", fontSize = 12.sp, color = Color.White)
                            }

                            if (logSuccessMsg != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(logSuccessMsg ?: "", color = HighDensitySuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        "Claim" -> {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = HighDensityOutline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text("DEDUCT REPAIR CLAIM FROM DEPOSIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityError)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Eligible active tenants with deposits
                            val eligibleTenants = deposits.filter { (it.amountDeposited - it.amountRefunded - it.amountClaimed) > 0 }
                            
                            if (eligibleTenants.isEmpty()) {
                                Text("No tenants currently have active security deposits to deduct claims from.", fontSize = 11.sp, color = HighDensitySecondaryText)
                            } else {
                                Text("Select Tenant to Deduct From", fontSize = 10.sp, color = HighDensitySecondaryText)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    eligibleTenants.forEach { t ->
                                        val isSel = claimTenantName == t.tenantName
                                        Box(
                                            modifier = Modifier
                                                .background(if (isSel) HighDensityError else Color.White, RoundedCornerShape(20.dp))
                                                .border(1.dp, if (isSel) HighDensityError else HighDensityOutline, RoundedCornerShape(20.dp))
                                                .clickable { claimTenantName = t.tenantName }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(t.tenantName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else HighDensityText)
                                        }
                                    }
                                }

                                val targetDeposit = eligibleTenants.find { it.tenantName == claimTenantName }
                                if (targetDeposit != null) {
                                    val availableEscrow = targetDeposit.amountDeposited - targetDeposit.amountRefunded - targetDeposit.amountClaimed
                                    Text(
                                        text = "Current safe escrow balance for tenant: KSh ${String.format("%,d", availableEscrow)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityPrimary,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = claimAmountStr,
                                    onValueChange = { claimAmountStr = it },
                                    label = { Text("Deduction Amount (KSh)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = HighDensityError,
                                        unfocusedBorderColor = HighDensityOutline
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = claimReason,
                                    onValueChange = { claimReason = it },
                                    label = { Text("Deduction Reason (Repairs / Painting / Bill)") },
                                    placeholder = { Text("e.g. Fixing broken kitchen cabinets") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = HighDensityError,
                                        unfocusedBorderColor = HighDensityOutline
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                val inputAmt = claimAmountStr.toIntOrNull()
                                val maxAllowed = targetDeposit?.let { it.amountDeposited - it.amountRefunded - it.amountClaimed } ?: 0
                                val isBtnEnabled = claimTenantName.isNotEmpty() && 
                                                   inputAmt != null && 
                                                   inputAmt > 0 && 
                                                   inputAmt <= maxAllowed && 
                                                   claimReason.isNotEmpty()

                                Button(
                                    onClick = {
                                        if (targetDeposit != null && inputAmt != null) {
                                            val index = deposits.indexOfFirst { it.tenantName == claimTenantName }
                                            if (index != -1) {
                                                val existing = deposits[index]
                                                val newClaimed = existing.amountClaimed + inputAmt
                                                val remaining = existing.amountDeposited - existing.amountRefunded - newClaimed
                                                
                                                deposits[index] = existing.copy(
                                                    amountClaimed = newClaimed,
                                                    status = if (remaining == 0) "Claimed in Full" else "Partially Refunded",
                                                    notes = "Claim of KSh $inputAmt deducted on $claimDateStr: $claimReason."
                                                )

                                                val randomId = "TXD-${(107..999).random()}"
                                                depositTransactions.add(
                                                    0,
                                                    DepositTransaction(
                                                        id = randomId,
                                                        tenantName = claimTenantName,
                                                        date = claimDateStr,
                                                        type = "Claim Deducted",
                                                        amount = inputAmt,
                                                        reason = claimReason
                                                    )
                                                )

                                                claimSuccessMsg = "Successfully deducted KSh $inputAmt for: $claimReason."
                                                claimTenantName = ""
                                                claimAmountStr = ""
                                                claimReason = ""
                                            }
                                        }
                                    },
                                    enabled = isBtnEnabled,
                                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityError),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Apply Reparation Claim", fontSize = 12.sp, color = Color.White)
                                }

                                if (claimSuccessMsg != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(claimSuccessMsg ?: "", color = HighDensitySuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        "Refund" -> {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = HighDensityOutline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text("DISBURSE REFUND TO VACATING TENANT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondary)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Eligible active tenants with deposits
                            val eligibleRefunds = deposits.filter { (it.amountDeposited - it.amountRefunded - it.amountClaimed) > 0 }
                            
                            if (eligibleRefunds.isEmpty()) {
                                Text("No tenants currently have active security deposits remaining to refund.", fontSize = 11.sp, color = HighDensitySecondaryText)
                            } else {
                                Text("Select Tenant to Refund", fontSize = 10.sp, color = HighDensitySecondaryText)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    eligibleRefunds.forEach { t ->
                                        val isSel = refundTenantName == t.tenantName
                                        Box(
                                            modifier = Modifier
                                                .background(if (isSel) HighDensitySecondary else Color.White, RoundedCornerShape(20.dp))
                                                .border(1.dp, if (isSel) HighDensitySecondary else HighDensityOutline, RoundedCornerShape(20.dp))
                                                .clickable {
                                                    refundTenantName = t.tenantName
                                                    // auto-fill available refund
                                                    val available = t.amountDeposited - t.amountRefunded - t.amountClaimed
                                                    refundAmountStr = available.toString()
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(t.tenantName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else HighDensityText)
                                        }
                                    }
                                }

                                val targetRefundDeposit = eligibleRefunds.find { it.tenantName == refundTenantName }
                                if (targetRefundDeposit != null) {
                                    val availableEscrow = targetRefundDeposit.amountDeposited - targetRefundDeposit.amountRefunded - targetRefundDeposit.amountClaimed
                                    Text(
                                        text = "Maximum available refund capacity: KSh ${String.format("%,d", availableEscrow)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityPrimary,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = refundAmountStr,
                                    onValueChange = { refundAmountStr = it },
                                    label = { Text("Refund Amount (KSh)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = HighDensitySecondary,
                                        unfocusedBorderColor = HighDensityOutline
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = refundNotes,
                                    onValueChange = { refundNotes = it },
                                    label = { Text("Refund Comments / Bank Ref") },
                                    placeholder = { Text("e.g. Account closed, key returned.") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = HighDensitySecondary,
                                        unfocusedBorderColor = HighDensityOutline
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                val inputRefundAmt = refundAmountStr.toIntOrNull()
                                val maxRefundAllowed = targetRefundDeposit?.let { it.amountDeposited - it.amountRefunded - it.amountClaimed } ?: 0
                                val isRefundEnabled = refundTenantName.isNotEmpty() && 
                                                      inputRefundAmt != null && 
                                                      inputRefundAmt > 0 && 
                                                      inputRefundAmt <= maxRefundAllowed

                                Button(
                                    onClick = {
                                        if (targetRefundDeposit != null && inputRefundAmt != null) {
                                            val index = deposits.indexOfFirst { it.tenantName == refundTenantName }
                                            if (index != -1) {
                                                val existing = deposits[index]
                                                val newRefunded = existing.amountRefunded + inputRefundAmt
                                                val remaining = existing.amountDeposited - newRefunded - existing.amountClaimed
                                                
                                                deposits[index] = existing.copy(
                                                    amountRefunded = newRefunded,
                                                    status = if (remaining == 0) "Fully Refunded" else "Partially Refunded",
                                                    notes = "Disbursed KSh $inputRefundAmt on $refundDateStr. notes: $refundNotes"
                                                )

                                                val randomId = "TXD-${(107..999).random()}"
                                                depositTransactions.add(
                                                    0,
                                                    DepositTransaction(
                                                        id = randomId,
                                                        tenantName = refundTenantName,
                                                        date = refundDateStr,
                                                        type = "Refund Issued",
                                                        amount = inputRefundAmt,
                                                        reason = refundNotes.ifEmpty { "Security deposit refunded to vacating tenant." }
                                                    )
                                                )

                                                refundSuccessMsg = "Successfully processed refund of KSh $inputRefundAmt to $refundTenantName."
                                                refundTenantName = ""
                                                refundAmountStr = ""
                                                refundNotes = ""
                                            }
                                        }
                                    },
                                    enabled = isRefundEnabled,
                                    colors = ButtonDefaults.buttonColors(containerColor = HighDensitySecondary),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Authorize Bank/Cash Refund", fontSize = 12.sp, color = Color.White)
                                }

                                if (refundSuccessMsg != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(refundSuccessMsg ?: "", color = HighDensitySuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Search Bar & Status Filters
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, HighDensityOutline),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("FILTERS & ESCROW SEARCH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText, letterSpacing = 1.sp)
                    
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by Tenant name or Unit...", fontSize = 12.sp, color = HighDensitySecondaryText) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = HighDensitySecondaryText, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = HighDensityPrimary,
                            unfocusedBorderColor = HighDensityOutline
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Held in Trust", "Partially Refunded", "Fully Refunded").forEach { f ->
                            val isSel = statusFilter == f
                            Box(
                                modifier = Modifier
                                    .background(if (isSel) HighDensityPrimary else HighDensitySurfaceVariant, RoundedCornerShape(16.dp))
                                    .border(1.dp, if (isSel) HighDensityPrimary else HighDensityOutline, RoundedCornerShape(16.dp))
                                    .clickable { statusFilter = f }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = f,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else HighDensitySecondaryText
                                )
                            }
                        }
                    }
                }
            }
        }

        // Escrow Deposits ledger cards
        item {
            Text(
                text = "Escrow Deposits Ledger (${filteredDeposits.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = HighDensityText
            )
        }

        if (filteredDeposits.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = HighDensitySurfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No matching security deposit registries found.", fontSize = 11.sp, color = HighDensitySecondaryText)
                    }
                }
            }
        } else {
            items(filteredDeposits) { entry ->
                var isExpanded by remember { mutableStateOf(false) }
                val netHeld = entry.amountDeposited - entry.amountRefunded - entry.amountClaimed

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HighDensityOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = entry.tenantName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                                Text(text = "Unit: ${entry.unit}", fontSize = 11.sp, color = HighDensitySecondaryText)
                            }

                            // Status badge
                            val (badgeBg, badgeText) = when (entry.status) {
                                "Held in Trust" -> HighDensityPrimaryContainer to HighDensityPrimary
                                "Partially Refunded" -> HighDensitySecondaryContainer to HighDensitySecondary
                                "Fully Refunded" -> Color(0xFFE2E8F0) to Color(0xFF64748B)
                                else -> Color(0xFFFEE2E2) to HighDensityError
                            }

                            Box(
                                modifier = Modifier
                                    .background(badgeBg, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = entry.status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = badgeText)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Ledger breakdown row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(HighDensitySurfaceVariant, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DEPOSITED", fontSize = 9.sp, color = HighDensitySecondaryText)
                                Text("KSh ${String.format("%,d", entry.amountDeposited)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CLAIMED", fontSize = 9.sp, color = HighDensitySecondaryText)
                                Text("KSh ${String.format("%,d", entry.amountClaimed)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityError)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("REFUNDED", fontSize = 9.sp, color = HighDensitySecondaryText)
                                Text("KSh ${String.format("%,d", entry.amountRefunded)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("NET SECURE HELD", fontSize = 9.sp, color = HighDensitySecondaryText)
                                Text("KSh ${String.format("%,d", netHeld)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                            }
                        }

                        // Expandable details block
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = HighDensityOutline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Escrow Ledger Timeline Details:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                            Text("Received Date: ${entry.dateDeposited}", fontSize = 11.sp, color = HighDensitySecondaryText)
                            Text("Latest Comments: ${entry.notes}", fontSize = 11.sp, color = HighDensitySecondaryText, modifier = Modifier.padding(top = 2.dp))

                            val specificTx = depositTransactions.filter { it.tenantName == entry.tenantName }
                            if (specificTx.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Historic Ledger entries for this escrow:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = HighDensitySecondaryText)
                                specificTx.forEach { tx ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = "${tx.date} — ${tx.type}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                                            Text(text = tx.reason, fontSize = 9.sp, color = HighDensitySecondaryText)
                                        }
                                        Text(
                                            text = "KSh ${String.format("%,d", tx.amount)}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (tx.type) {
                                                "Deposit Received" -> HighDensityPrimary
                                                "Refund Issued" -> HighDensitySecondary
                                                else -> HighDensityError
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Global Transaction History Log
        item {
            Text(
                text = "Recent Escrow Transactions Feed",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = HighDensityText
            )
        }

        items(depositTransactions) { tx ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, HighDensityOutline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (txBg, txColor, txIcon) = when (tx.type) {
                        "Deposit Received" -> Triple(HighDensityPrimaryContainer, HighDensityPrimary, Icons.Default.Savings)
                        "Refund Issued" -> Triple(HighDensitySecondaryContainer, HighDensitySecondary, Icons.Default.SettingsBackupRestore)
                        else -> Triple(Color(0xFFFEE2E2), HighDensityError, Icons.Default.Handyman)
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(txBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(txIcon, contentDescription = null, tint = txColor, modifier = Modifier.size(16.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = tx.tenantName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HighDensityText)
                            Text(text = tx.date, fontSize = 10.sp, color = HighDensitySecondaryText)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = tx.reason, fontSize = 11.sp, color = HighDensitySecondaryText, modifier = Modifier.padding(top = 1.dp))
                            Text(
                                text = "KSh ${String.format("%,d", tx.amount)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = txColor
                            )
                        }
                    }
                }
            }
        }
    }
}

