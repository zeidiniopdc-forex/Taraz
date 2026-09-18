package com.taraz.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import java.text.NumberFormat
import java.util.Locale

private val Blue = Color(0xFF2D66E5)
private val Bg = Color(0xFFF8F9FC)
private val Card = Color.White
private val Green = Color(0xFF14966C)
private val Red = Color(0xFFD93D55)

@Stable
data class Tx(
    val id: Long,
    val title: String,
    val amount: Long,
    val income: Boolean,
    val date: String,
    val category: String,
    val account: String
)

class Db(context: Context) {
    private val p = context.getSharedPreferences("taraz_db", Context.MODE_PRIVATE)

    fun load(): List<Tx> = p.getString("tx", "")!!.split("~")
        .filter { it.isNotBlank() }
        .mapNotNull {
            val x = it.split("|")
            if (x.size < 7) null else Tx(x[0].toLong(), x[1], x[2].toLong(), x[3].toBoolean(), x[4], x[5], x[6])
        }.sortedByDescending { it.id }

    fun save(list: List<Tx>) = p.edit().putString(
        "tx",
        list.joinToString("~") { listOf(it.id, it.title, it.amount, it.income, it.date, it.category, it.account).joinToString("|") }
    ).apply()
}

class MainActivity : ComponentActivity() {
    private val smsPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Db(this)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TarazApp(db)
            }
        }
    }

    fun requestSms() = smsPermission.launch(arrayOf("android.permission.READ_SMS", "android.permission.RECEIVE_SMS"))
}

@Composable
fun TarazApp(db: Db) {
    var screen by remember { mutableStateOf("home") }
    var txs by remember { mutableStateOf(db.load()) }
    fun add(tx: Tx) {
        txs = listOf(tx) + txs
        db.save(txs)
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, background = Bg, surface = Card)) {
        Scaffold(containerColor = Bg, bottomBar = { BottomBar(screen) { screen = it } }) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (screen) {
                    "home" -> Dashboard(txs) { screen = "add" }
                    "tx" -> Transactions(txs) { screen = "add" }
                    "add" -> AddTransaction({ add(it); screen = "tx" }) { screen = "home" }
                    "sms" -> SmsPage { add(it); screen = "tx" }
                    "more" -> MorePage()
                }
            }
        }
    }
}

@Composable
fun BottomBar(screen: String, onNav: (String) -> Unit) {
    NavigationBar(containerColor = Color(0xFFEAF0F6)) {
        listOf(
            "home" to ("داشبورد مالی" to Icons.Default.Dashboard),
            "tx" to ("تراکنش‌ها" to Icons.Default.ReceiptLong),
            "add" to ("ثبت تراکنش" to Icons.Default.AddCircle),
            "sms" to ("پیامک بانک" to Icons.Default.Sms),
            "more" to ("امکانات" to Icons.Default.GridView)
        ).forEach { (route, item) ->
            NavigationBarItem(
                selected = screen == route,
                onClick = { onNav(route) },
                icon = { Icon(item.second, null) },
                label = { Text(item.first, fontSize = 11.sp) }
            )
        }
    }
}

@Composable
fun Header(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowForward, null) }
        else Spacer(Modifier.size(48.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            subtitle?.let { Text(it, color = Color.Gray, fontSize = 13.sp) }
        }
        Icon(Icons.Default.Tune, null, tint = Color(0xFF63718A))
    }
}

@Composable
fun Dashboard(txs: List<Tx>, onAdd: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("داشبورد مالی", "ابزار آنلاین برای مدیریت درآمد، هزینه، بودجه و گزارش‌ها")
        CardBox(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoGraph, null, tint = Blue, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("راهنمای سریع داشبورد", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("مانده یعنی پولی که الان در حساب‌ها داری. درآمد پولی است که وارد می‌شود و هزینه پولی است که از حساب خارج می‌شود.", color = Color(0xFF667085), fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val income = txs.filter { it.income }.sumOf { it.amount }
        val expense = txs.filter { !it.income }.sumOf { it.amount }
        val balance = income - expense
        Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(Blue, RoundedCornerShape(18.dp)).padding(22.dp)) {
            Column {
                Text("مانده فعلی", color = Color.White.copy(.8f))
                Text(money(balance), color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Stat("درآمد", income, Icons.Default.ArrowDownward)
                    Stat("هزینه", expense, Icons.Default.ArrowUpward)
                }
                Spacer(Modifier.height(14.dp))
                Button(onClick = onAdd, modifier = Modifier.align(Alignment.End), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C7CEB))) {
                    Icon(Icons.Default.Add, null)
                    Text("ثبت اولین تراکنش")
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        CardBox(Modifier.padding(horizontal = 20.dp)) {
            Text("چک‌لیست شروع", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("با چند قدم ساده گزارش‌های دقیق و قابل استفاده بساز.", color = Color.Gray)
            Spacer(Modifier.height(14.dp))
            listOf("ساخت اولین حساب" to Icons.Default.AccountBalanceWallet, "ثبت اولین تراکنش" to Icons.Default.ReceiptLong, "گرفتن پشتیبان" to Icons.Default.CloudUpload).forEachIndexed { i, (t, ic) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 7.dp)) {
                    Icon(ic, null, tint = if (i < 2) Blue else Color.Gray)
                    Spacer(Modifier.width(10.dp))
                    Text(t, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
fun Stat(title: String, value: Long, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(.12f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color.White)
                Spacer(Modifier.width(5.dp))
                Text(title, color = Color.White)
            }
            Text(money(value), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun Transactions(txs: List<Tx>, onAdd: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header("تراکنش‌ها", "ثبت و مدیریت درآمدها و هزینه‌ها")
        LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(txs) { tx ->
                CardBox(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (tx.income) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, null, tint = if (tx.income) Green else Red, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tx.title, fontWeight = FontWeight.Bold)
                            Text("${tx.category} • ${tx.date} • ${tx.account}", color = Color.Gray, fontSize = 12.sp)
                        }
                        Text((if (tx.income) "+ " else "- ") + money(tx.amount), fontWeight = FontWeight.Bold, color = if (tx.income) Green else Red)
                    }
                }
            }
        }
        FloatingActionButton(onClick = onAdd, containerColor = Blue, contentColor = Color.White, modifier = Modifier.padding(20.dp).align(Alignment.Start)) {
            Icon(Icons.Default.Add, null)
        }
    }
}

@Composable
fun AddTransaction(onSave: (Tx) -> Unit, onCancel: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var income by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("عمومی") }
    var account by remember { mutableStateOf("حساب اصلی") }
    Column(Modifier.fillMaxSize()) {
        Header("ثبت تراکنش", onBack = onCancel)
        Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            Text("نوع تراکنش", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = income, onClick = { income = true }, label = { Text("درآمد") })
                FilterChip(selected = !income, onClick = { income = false }, label = { Text("هزینه") })
            }
            Spacer(Modifier.height(14.dp))
            Field("عنوان", title) { title = it }
            Field("مبلغ (ریال)", amount) { amount = it }
            Field("دسته‌بندی", category) { category = it }
            Field("حساب", account) { account = it }
            Spacer(Modifier.height(18.dp))
            Button(onClick = {
                amount.toLongOrNull()?.let {
                    onSave(Tx(System.currentTimeMillis(), if (title.isBlank()) "تراکنش جدید" else title, it, income, "۱۴۰۵/۰۶/۲۸", category, account))
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = amount.toLongOrNull() != null) { Text("ذخیره تراکنش") }
        }
    }
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), singleLine = true)
}

@Composable
fun SmsPage(onSave: (Tx) -> Unit) {
    var sms by remember { mutableStateOf("") }
    var analyzed by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("ثبت از پیامک بانک")
        CardBox(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = true, onCheckedChange = { })
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("اعلان هنگام دریافت پیامک بانکی", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("تا پیامک برداشت یا خرید بیاید، اعلان با مبلغ همان پیامک ثبت می‌شود.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        CardBox(Modifier.padding(horizontal = 20.dp)) {
            Text("تست با متن پیامک", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("متن پیامک بانک", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(sms, { sms = it }, Modifier.fillMaxWidth().height(120.dp))
            Spacer(Modifier.height(10.dp))
            Button(onClick = { analyzed = true }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AutoAwesome, null)
                Text("تحلیل متن پیامک")
            }
        }
        if (analyzed) {
            val amount = Regex("([0-9]{1,3}(?:[,،][0-9]{3})+|[0-9]{4,})").find(sms)?.value?.replace(",", "")?.replace("،", "")?.toLongOrNull()
            CardBox(Modifier.padding(20.dp)) {
                Text("تراکنش‌های پیشنهادی آماده بررسی است", fontWeight = FontWeight.Bold)
                Text(if (amount != null) "مبلغ شناسایی‌شده: ${money(amount)}" else "مبلغی با اطمینان کافی شناسایی نشد.", color = Color.Gray)
                if (amount != null) Button(onClick = { onSave(Tx(System.currentTimeMillis(), "تراکنش پیامکی", amount, false, "۱۴۰۵/۰۶/۲۸", "بانکی", "حساب اصلی")) }) { Text("ثبت مبلغ پیشنهادی") }
            }
        }
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
fun MorePage() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("امکانات")
        listOf("بودجه‌بندی هوشمند", "گزارش درآمد و هزینه", "حساب‌ها و کارت‌ها", "پشتیبان‌گیری", "تنظیمات اعلان پیامک").forEach {
            CardBox(Modifier.padding(horizontal = 20.dp, vertical = 5.dp).clickable { }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChevronLeft, null, tint = Blue)
                    Spacer(Modifier.width(8.dp))
                    Text(it, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Card), border = BorderStroke(1.dp, Color(0xFFE5E7EB))) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

fun money(n: Long): String = NumberFormat.getNumberInstance(Locale.US).format(n) + " ریال"
