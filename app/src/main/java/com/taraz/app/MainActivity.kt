package com.taraz.app

import android.content.Context
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private val Blue = Color(0xFF2864E8)
private val BlueSoft = Color(0xFFEAF1FF)
private val Green = Color(0xFF16A579)
private val GreenSoft = Color(0xFFE7F8F1)
private val Red = Color(0xFFE45763)
private val RedSoft = Color(0xFFFFECEE)
private val Purple = Color(0xFF7956D8)
private val PurpleSoft = Color(0xFFF0EBFF)
private val Orange = Color(0xFFF39A2F)
private val OrangeSoft = Color(0xFFFFF3DF)
private val Teal = Color(0xFF1499A8)
private val TealSoft = Color(0xFFE6F7F9)
private val Ink = Color(0xFF182033)
private val Muted = Color(0xFF697386)
private val Bg = Color(0xFFF6F8FC)

private val Vazirmatn = FontFamily(
    Font(com.taraz.app.R.font.vazirmatn_regular, FontWeight.Normal),
    Font(com.taraz.app.R.font.vazirmatn_medium, FontWeight.Medium),
    Font(com.taraz.app.R.font.vazirmatn_bold, FontWeight.Bold)
)

private val TarazTypography = Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    headlineLarge = androidx.compose.ui.text.TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    headlineMedium = androidx.compose.ui.text.TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    titleLarge = androidx.compose.ui.text.TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold),
    titleMedium = androidx.compose.ui.text.TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium),
    bodyLarge = androidx.compose.ui.text.TextStyle(fontFamily = Vazirmatn),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontFamily = Vazirmatn),
    labelLarge = androidx.compose.ui.text.TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Medium),
    labelMedium = androidx.compose.ui.text.TextStyle(fontFamily = Vazirmatn)
)

data class Tx(val id: Long, val title: String, val amount: Long, val income: Boolean, val date: String, val category: String, val account: String)
data class BankAccount(val id: Long, val bankName: String, val accountName: String, val accountNumber: String, val cardNumber: String, val senderNumber: String, val smsPattern: String, val balance: Long)

private fun enc(value: String): String = Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
private fun dec(value: String): String = runCatching { String(Base64.decode(value, Base64.NO_WRAP), Charsets.UTF_8) }.getOrDefault("")

class Db(context: Context) {
    private val p = context.getSharedPreferences("taraz_db", Context.MODE_PRIVATE)
    fun loadTx(): List<Tx> = p.getString("tx", "")!!.split("~").filter { it.isNotBlank() }.mapNotNull { val x = it.split("|"); if (x.size == 7) Tx(x[0].toLongOrNull() ?: return@mapNotNull null, x[1], x[2].toLongOrNull() ?: return@mapNotNull null, x[3].toBoolean(), x[4], x[5], x[6]) else null }.sortedByDescending { it.id }
    fun saveTx(list: List<Tx>) = p.edit().putString("tx", list.joinToString("~") { listOf(it.id, it.title, it.amount, it.income, it.date, it.category, it.account).joinToString("|") }).apply()
    fun loadAccounts(): List<BankAccount> = p.getString("accounts", "")!!.split("~").filter { it.isNotBlank() }.mapNotNull { row -> val x = row.split("|"); if (x.size != 8) return@mapNotNull null; BankAccount(x[0].toLongOrNull() ?: return@mapNotNull null, dec(x[1]), dec(x[2]), dec(x[3]), dec(x[4]), dec(x[5]), dec(x[6]), x[7].toLongOrNull() ?: 0L) }.sortedByDescending { it.id }
    fun saveAccounts(list: List<BankAccount>) = p.edit().putString("accounts", list.joinToString("~") { listOf(it.id, enc(it.bankName), enc(it.accountName), enc(it.accountNumber), enc(it.cardNumber), enc(it.senderNumber), enc(it.smsPattern), it.balance).joinToString("|") }).apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { TarazApp(Db(this)) } } }
}

@Composable
fun TarazApp(db: Db) {
    var page by remember { mutableStateOf("home") }
    var txs by remember { mutableStateOf(db.loadTx()) }
    var accounts by remember { mutableStateOf(db.loadAccounts()) }
    fun addTx(tx: Tx) { txs = listOf(tx) + txs; db.saveTx(txs) }
    fun addAccount(account: BankAccount) { accounts = listOf(account) + accounts; db.saveAccounts(accounts) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, background = Bg, surface = Color.White), typography = TarazTypography) {
        Scaffold(containerColor = Bg, bottomBar = { BottomNav(page) { page = it } }) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (page) {
                    "home" -> Home(txs, accounts, { page = "add" }, { page = "accounts" }, { page = "sms" })
                    "tx" -> Transactions(txs) { page = "add" }
                    "add" -> AddTransaction({ addTx(it); page = "tx" }) { page = "home" }
                    "sms" -> SmsPage(accounts) { addTx(it); page = "tx" }
                    "accounts" -> AccountsPage(accounts, { page = "newAccount" }, { page = "sms" })
                    "newAccount" -> AddAccountPage({ addAccount(it); page = "accounts" }) { page = "accounts" }
                    else -> MorePage { route -> page = route }
                }
            }
        }
    }
}

@Composable
fun BottomNav(page: String, go: (String) -> Unit) { NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) { listOf(Triple("home", "داشبورد", Icons.Default.Dashboard), Triple("tx", "تراکنش‌ها", Icons.Default.ReceiptLong), Triple("add", "ثبت", Icons.Default.AddCircle), Triple("sms", "پیامک", Icons.Default.Sms), Triple("more", "امکانات", Icons.Default.GridView)).forEach { (route, title, icon) -> NavigationBarItem(selected = page == route, onClick = { go(route) }, icon = { Box(Modifier.clip(RoundedCornerShape(14.dp)).background(if (page == route) BlueSoft else Color.Transparent).padding(horizontal = 10.dp, vertical = 4.dp)) { Icon(icon, null, tint = if (page == route) Blue else Muted) } }, label = { Text(title, fontSize = 11.sp, color = if (page == route) Blue else Muted) }, colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)) } } }

@Composable
fun Header(title: String, subtitle: String? = null, back: (() -> Unit)? = null) { Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { if (back != null) IconButton(onClick = back) { Icon(Icons.Default.ArrowForward, null, tint = Ink) } else Spacer(Modifier.size(48.dp)); Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(title, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink); if (subtitle != null) Text(subtitle, color = Muted, fontSize = 12.sp) }; Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { Icon(Icons.Default.Tune, null, tint = Muted) } } }

@Composable
fun SectionTitle(title: String, action: String? = null, onAction: (() -> Unit)? = null) { Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink); if (action != null) Text(action, color = Blue, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = if (onAction != null) Modifier.clickable { onAction() } else Modifier) } }

@Composable
fun Home(txs: List<Tx>, accounts: List<BankAccount>, onAdd: () -> Unit, onAccounts: () -> Unit, onSms: () -> Unit) {
    val income = txs.filter { it.income }.sumOf { it.amount }; val expense = txs.filter { !it.income }.sumOf { it.amount }; val accountBalance = accounts.sumOf { it.balance }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("داشبورد مالی", "مدیریت هوشمند درآمد، هزینه و حساب‌ها")
        Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Blue)) { Column(Modifier.padding(22.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("مانده کل", color = Color.White.copy(.78f), fontSize = 14.sp); Text(money(accountBalance + income - expense), color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold) }; Box(Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(.16f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White) } }; Spacer(Modifier.height(18.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { SummaryPill("درآمد", income, Icons.Default.ArrowUpward, Modifier.weight(1f)); SummaryPill("هزینه", expense, Icons.Default.ArrowDownward, Modifier.weight(1f)) }; Spacer(Modifier.height(16.dp)); Button(onClick = onAdd, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Blue), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("ثبت تراکنش جدید") } } }
        SectionTitle("دسترسی سریع")
        Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { QuickCard("بودجه", "مدیریت بودجه", Icons.Default.PieChart, Purple, PurpleSoft, Modifier.weight(1f)) { }; QuickCard("حساب‌ها", "${accounts.size} حساب", Icons.Default.AccountBalance, Teal, TealSoft, Modifier.weight(1f), onAccounts); QuickCard("پیامک", "الگوی بانکی", Icons.Default.Sms, Orange, OrangeSoft, Modifier.weight(1f), onSms) }
        Spacer(Modifier.height(8.dp))
        Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth().clickable { onAccounts() }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = TealSoft), border = BorderStroke(1.dp, Teal.copy(.18f))) { Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(46.dp).clip(CircleShape).background(Teal.copy(.13f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountBalance, null, tint = Teal) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("حساب‌ها و کارت‌ها", fontWeight = FontWeight.Bold, color = Ink); Text("مدیریت حساب، سرشماره و الگوی پیامک", color = Muted, fontSize = 11.sp) }; Icon(Icons.Default.ChevronLeft, null, tint = Teal) } }
        SectionTitle("آخرین تراکنش‌ها", "مشاهده همه")
        if (txs.isEmpty()) EmptyCard() else txs.take(4).forEach { TransactionCard(it) }
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
fun SummaryPill(title: String, value: Long, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) { Row(modifier.background(Color.White.copy(.13f), RoundedCornerShape(14.dp)).padding(11.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(7.dp)); Column { Text(title, color = Color.White.copy(.72f), fontSize = 12.sp); Text(money(value), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) } } }

@Composable
fun QuickCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, soft: Color, modifier: Modifier, onClick: () -> Unit) { Card(modifier.clickable { onClick() }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = soft), border = BorderStroke(1.dp, tint.copy(.12f))) { Column(Modifier.padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(40.dp).clip(CircleShape).background(tint.copy(.14f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint) }; Spacer(Modifier.height(7.dp)); Text(title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 14.sp); Text(subtitle, color = Muted, fontSize = 10.sp) } } }

@Composable
fun EmptyCard() { Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.ReceiptLong, null, tint = Blue, modifier = Modifier.size(36.dp)); Text("هنوز تراکنشی ثبت نشده", fontWeight = FontWeight.Bold, color = Ink); Text("اولین تراکنش خود را ثبت کنید.", color = Muted, fontSize = 12.sp) } } }

@Composable
fun TransactionCard(tx: Tx) { val tint = if (tx.income) Green else Red; val soft = if (tx.income) GreenSoft else RedSoft; Card(Modifier.padding(horizontal = 20.dp, vertical = 4.dp).fillMaxWidth(), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE9ECF2))) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(44.dp).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) { Icon(if (tx.income) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, null, tint = tint) }; Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(tx.title, fontWeight = FontWeight.Medium, color = Ink); Text("${tx.category} • ${tx.date}", color = Muted, fontSize = 11.sp) }; Text((if (tx.income) "+" else "-") + money(tx.amount), color = tint, fontWeight = FontWeight.Bold, fontSize = 13.sp) } } }

@Composable
fun Transactions(txs: List<Tx>, onAdd: () -> Unit) { Column(Modifier.fillMaxSize()) { Header("تراکنش‌ها", "همه درآمدها و هزینه‌های ثبت‌شده"); Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(true, {}, label = { Text("همه") }); FilterChip(false, {}, label = { Text("درآمد") }); FilterChip(false, {}, label = { Text("هزینه") }) }; LazyColumn(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) { items(txs) { TransactionCard(it) } }; FloatingActionButton(onClick = onAdd, modifier = Modifier.padding(18.dp).align(Alignment.Start), containerColor = Blue, contentColor = Color.White) { Icon(Icons.Default.Add, null) } } }

@Composable
fun AddTransaction(onSave: (Tx) -> Unit, onBack: () -> Unit) { var title by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }; var income by remember { mutableStateOf(false) }; var category by remember { mutableStateOf("عمومی") }; var account by remember { mutableStateOf("حساب اصلی") }; Column(Modifier.fillMaxSize()) { Header("ثبت تراکنش", "اطلاعات تراکنش را وارد کنید", onBack); Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (income) GreenSoft else RedSoft)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (income) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, null, tint = if (income) Green else Red); Spacer(Modifier.width(10.dp)); Column { Text(if (income) "ثبت درآمد" else "ثبت هزینه", fontWeight = FontWeight.Bold); Text("نوع تراکنش را انتخاب کنید", color = Muted, fontSize = 11.sp) } } }; Spacer(Modifier.height(12.dp)); Row { FilterChip(selected = income, onClick = { income = true }, label = { Text("درآمد") }); Spacer(Modifier.width(8.dp)); FilterChip(selected = !income, onClick = { income = false }, label = { Text("هزینه") }) }; Field("عنوان", title) { title = it }; Field("مبلغ (ریال)", amount, KeyboardType.Number) { amount = it }; Field("دسته‌بندی", category) { category = it }; Field("حساب", account) { account = it }; Spacer(Modifier.height(14.dp)); Button(onClick = { amount.toLongOrNull()?.let { onSave(Tx(System.currentTimeMillis(), title.ifBlank { "تراکنش جدید" }, it, income, "۱۴۰۵/۰۶/۲۸", category, account)) } }, modifier = Modifier.fillMaxWidth(), enabled = amount.toLongOrNull() != null, shape = RoundedCornerShape(14.dp)) { Text("ذخیره تراکنش") } } } }

@Composable
fun Field(label: String, value: String, keyboard: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) { OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), singleLine = true, shape = RoundedCornerShape(14.dp), keyboardOptions = KeyboardOptions(keyboardType = keyboard)) }

@Composable
fun AccountsPage(accounts: List<BankAccount>, onNew: () -> Unit, onSms: () -> Unit) { Column(Modifier.fillMaxSize()) { Header("حساب‌ها و کارت‌ها", "مدیریت حساب و تنظیمات پیامک بانکی"); Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = onNew, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(5.dp)); Text("ایجاد حساب") }; OutlinedButton(onClick = onSms, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Sms, null); Spacer(Modifier.width(5.dp)); Text("الگوهای پیامک") } }; Spacer(Modifier.height(10.dp)); if (accounts.isEmpty()) { Card(Modifier.padding(20.dp).fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = TealSoft)) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AccountBalanceWallet, null, tint = Teal, modifier = Modifier.size(50.dp)); Text("هنوز حسابی ثبت نشده", fontWeight = FontWeight.Bold, color = Ink); Text("حساب بانکی خود را اضافه کنید و سرشماره و الگوی پیامک آن را تنظیم کنید.", color = Muted, fontSize = 12.sp) } } } else { LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)) { items(accounts) { AccountCard(it) } } } } }

@Composable
fun AccountCard(account: BankAccount) { Card(Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Teal.copy(.14f))) { Column(Modifier.padding(17.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).clip(CircleShape).background(TealSoft), contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountBalance, null, tint = Teal) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(account.accountName.ifBlank { account.bankName }, fontWeight = FontWeight.Bold, color = Ink, fontSize = 16.sp); Text(account.bankName, color = Muted, fontSize = 11.sp) }; Text(money(account.balance), color = Teal, fontWeight = FontWeight.Bold, fontSize = 13.sp) }; Spacer(Modifier.height(12.dp)); Divider(color = Color(0xFFE8EDF3)); Spacer(Modifier.height(10.dp)); Text("شماره حساب: ${mask(account.accountNumber)}", color = Ink, fontSize = 12.sp); Text("کارت: ${mask(account.cardNumber)}", color = Muted, fontSize = 11.sp); Spacer(Modifier.height(5.dp)); Text("سرشماره پیامک: ${account.senderNumber.ifBlank { "ثبت نشده" }}", color = Orange, fontWeight = FontWeight.Medium, fontSize = 12.sp); Text("الگوی پیامک: ${account.smsPattern.ifBlank { "ثبت نشده" }}", color = Muted, fontSize = 11.sp, maxLines = 2) } } }

@Composable
fun AddAccountPage(onSave: (BankAccount) -> Unit, onBack: () -> Unit) { var bank by remember { mutableStateOf("") }; var name by remember { mutableStateOf("") }; var accountNo by remember { mutableStateOf("") }; var cardNo by remember { mutableStateOf("") }; var sender by remember { mutableStateOf("") }; var pattern by remember { mutableStateOf("") }; var balance by remember { mutableStateOf("0") }; Column(Modifier.fillMaxSize()) { Header("ایجاد حساب", "حساب بانکی و الگوی پیامک را ثبت کنید", onBack); Column(Modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = TealSoft)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AccountBalance, null, tint = Teal); Spacer(Modifier.width(10.dp)); Column { Text("تعریف حساب بانکی", fontWeight = FontWeight.Bold, color = Ink); Text("اطلاعات برای تشخیص تراکنش‌های پیامکی ذخیره می‌شود.", color = Muted, fontSize = 11.sp) } } }; Spacer(Modifier.height(10.dp)); Field("نام بانک", bank) { bank = it }; Field("نام حساب", name) { name = it }; Field("شماره حساب", accountNo, KeyboardType.Number) { accountNo = it }; Field("شماره کارت", cardNo, KeyboardType.Number) { cardNo = it }; Field("سرشماره / شماره ارسال‌کننده پیامک", sender) { sender = it }; OutlinedTextField(value = pattern, onValueChange = { pattern = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(125.dp), label = { Text("الگوی متن پیامک") }, placeholder = { Text("مثال: برداشت از حساب ... مبلغ ... مانده ...") }, shape = RoundedCornerShape(14.dp)); Field("موجودی اولیه (ریال)", balance, KeyboardType.Number) { balance = it }; Spacer(Modifier.height(10.dp)); Button(onClick = { onSave(BankAccount(System.currentTimeMillis(), bank.ifBlank { "بانک" }, name.ifBlank { bank }, accountNo, cardNo, sender, pattern, balance.toLongOrNull() ?: 0L)) }, enabled = bank.isNotBlank() && (name.isNotBlank() || accountNo.isNotBlank()), modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(6.dp)); Text("ذخیره حساب") }; Spacer(Modifier.height(90.dp)) } } }

@Composable
fun SmsPage(accounts: List<BankAccount>, onSave: (Tx) -> Unit) { var text by remember { mutableStateOf("") }; var analyzed by remember { mutableStateOf(false) }; Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { Header("پیامک بانک", "تشخیص و ثبت تراکنش از متن پیامک"); Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = OrangeSoft)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(46.dp).clip(CircleShape).background(Orange.copy(.15f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.NotificationsActive, null, tint = Orange) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("تشخیص پیامک بانکی", fontWeight = FontWeight.Bold, color = Ink); Text("${accounts.size} حساب دارای تنظیمات پیامک", color = Muted, fontSize = 11.sp) }; Switch(true, {}) } }; Spacer(Modifier.height(12.dp)); SectionTitle("تست با متن پیامک"); Card(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(18.dp)) { OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth().height(130.dp), label = { Text("متن پیامک بانک") }, shape = RoundedCornerShape(14.dp)); Spacer(Modifier.height(10.dp)); Button(onClick = { analyzed = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(5.dp)); Text("تحلیل متن پیامک") } } }; if (analyzed) { val amount = Regex("([0-9]{1,3}(?:[,،][0-9]{3})+|[0-9]{4,})").find(text)?.value?.replace(",", "")?.replace("،", "")?.toLongOrNull(); Card(Modifier.padding(20.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = TealSoft)) { Column(Modifier.padding(18.dp)) { Text("نتیجه تحلیل", fontWeight = FontWeight.Bold); Text(if (amount != null) "مبلغ شناسایی‌شده: ${money(amount)}" else "مبلغی شناسایی نشد.", color = Muted); if (amount != null) Button(onClick = { onSave(Tx(System.currentTimeMillis(), "تراکنش پیامکی", amount, false, "۱۴۰۵/۰۶/۲۸", "بانکی", accounts.firstOrNull()?.accountName ?: "حساب اصلی")) }) { Text("ثبت مبلغ پیشنهادی") } } } }; Spacer(Modifier.height(90.dp)) } }

@Composable
fun MorePage(go: (String) -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { Header("امکانات", "ابزارهای تکمیلی تراز"); MoreRow("حساب‌ها و کارت‌ها", "ایجاد حساب و تنظیم سرشماره و الگوی پیامک", Icons.Default.AccountBalance, Teal) { go("accounts") }; MoreRow("بودجه‌بندی هوشمند", "مدیریت بودجه ماهانه", Icons.Default.PieChart, Purple) { }; MoreRow("گزارش درآمد و هزینه", "گزارش و تحلیل مالی", Icons.Default.BarChart, Blue) { }; MoreRow("پیامک‌های بانکی", "تشخیص و ثبت تراکنش", Icons.Default.Sms, Orange) { go("sms") }; MoreRow("پشتیبان‌گیری", "ذخیره و بازیابی اطلاعات", Icons.Default.Backup, Green) { }; Spacer(Modifier.height(90.dp)) } }

@Composable
fun MoreRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) { Card(Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(44.dp).clip(CircleShape).background(tint.copy(.12f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink); Text(subtitle, color = Muted, fontSize = 11.sp) }; Icon(Icons.Default.ChevronLeft, null, tint = Muted) } } }

fun mask(value: String): String = if (value.length <= 4) value else "••••" + value.takeLast(4)
fun money(value: Long): String = NumberFormat.getNumberInstance(Locale.US).format(value) + " ریال"
