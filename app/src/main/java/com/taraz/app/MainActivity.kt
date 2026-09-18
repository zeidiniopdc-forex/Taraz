package com.taraz.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
private val Green = Color(0xFF14966C)
private val Red = Color(0xFFD93D55)

data class Tx(val id: Long, val title: String, val amount: Long, val income: Boolean, val date: String, val category: String, val account: String)

class Db(context: Context) {
    private val p = context.getSharedPreferences("taraz_db", Context.MODE_PRIVATE)
    fun load(): List<Tx> = p.getString("tx", "")!!.split("~").filter { it.isNotBlank() }.mapNotNull {
        val x = it.split("|")
        if (x.size == 7) Tx(x[0].toLongOrNull() ?: return@mapNotNull null, x[1], x[2].toLongOrNull() ?: return@mapNotNull null, x[3].toBoolean(), x[4], x[5], x[6]) else null
    }.sortedByDescending { it.id }
    fun save(list: List<Tx>) = p.edit().putString("tx", list.joinToString("~") { listOf(it.id, it.title, it.amount, it.income, it.date, it.category, it.account).joinToString("|") }).apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { TarazApp(Db(this)) }
        }
    }
}

@Composable
fun TarazApp(db: Db) {
    var page by remember { mutableStateOf("home") }
    var txs by remember { mutableStateOf(db.load()) }
    fun addTx(tx: Tx) { txs = listOf(tx) + txs; db.save(txs) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, background = Bg)) {
        Scaffold(containerColor = Bg, bottomBar = { BottomNav(page) { page = it } }) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (page) {
                    "home" -> Home(txs) { page = "add" }
                    "tx" -> Transactions(txs) { page = "add" }
                    "add" -> AddTransaction({ addTx(it); page = "tx" }) { page = "home" }
                    "sms" -> SmsPage { addTx(it); page = "tx" }
                    else -> MorePage()
                }
            }
        }
    }
}

@Composable
fun BottomNav(page: String, go: (String) -> Unit) {
    NavigationBar(containerColor = Color(0xFFEAF0F6)) {
        val items = listOf(
            Triple("home", "داشبورد مالی", Icons.Default.Dashboard),
            Triple("tx", "تراکنش‌ها", Icons.Default.ReceiptLong),
            Triple("add", "ثبت تراکنش", Icons.Default.AddCircle),
            Triple("sms", "پیامک بانک", Icons.Default.Sms),
            Triple("more", "امکانات", Icons.Default.GridView)
        )
        items.forEach { (route, title, icon) ->
            NavigationBarItem(
                selected = page == route,
                onClick = { go(route) },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(title, fontSize = 11.sp) }
            )
        }
    }
}

@Composable
fun Header(title: String, subtitle: String? = null, back: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        if (back != null) IconButton(onClick = back) { Icon(Icons.Default.ArrowForward, contentDescription = null) } else Spacer(Modifier.size(48.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF63718A))
    }
}

@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFE5E7EB))) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun Home(txs: List<Tx>, onAdd: () -> Unit) {
    val income = txs.filter { it.income }.sumOf { it.amount }
    val expense = txs.filter { !it.income }.sumOf { it.amount }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("داشبورد مالی", "ابزار آنلاین برای مدیریت درآمد، هزینه، بودجه و گزارش‌ها")
        CardBox(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoGraph, contentDescription = null, tint = Blue, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("راهنمای سریع داشبورد", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("مانده یعنی پولی که الان در حساب‌ها داری. درآمد وارد می‌شود و هزینه از حساب خارج می‌شود.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        CardBox(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
            Text("مانده فعلی", color = Blue, fontWeight = FontWeight.Bold)
            Text(money(income - expense), fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Metric("درآمد", income, Green, Modifier.weight(1f))
                Metric("هزینه", expense, Red, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("ثبت تراکنش") }
        }
        Spacer(Modifier.height(18.dp))
        CardBox(Modifier.padding(horizontal = 20.dp)) {
            Text("چک‌لیست شروع", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("با چند قدم ساده گزارش‌های دقیق بساز.", color = Color.Gray)
            listOf("ساخت اولین حساب" to Icons.Default.AccountBalanceWallet, "ثبت اولین تراکنش" to Icons.Default.ReceiptLong, "گرفتن پشتیبان" to Icons.Default.CloudUpload).forEach { item ->
                Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(item.second, contentDescription = null, tint = Blue); Spacer(Modifier.width(8.dp)); Text(item.first)
                }
            }
        }
        Spacer(Modifier.height(90.dp))
    }
}

@Composable
fun Metric(title: String, value: Long, tint: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.12f))) {
        Column(Modifier.padding(12.dp)) { Text(title, color = tint); Text(money(value), fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun Transactions(txs: List<Tx>, onAdd: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header("تراکنش‌ها", "ثبت و مدیریت درآمدها و هزینه‌ها")
        LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(txs) { tx ->
                CardBox(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (tx.income) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, null, tint = if (tx.income) Green else Red)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Text(tx.title, fontWeight = FontWeight.Bold); Text("${tx.category} • ${tx.date} • ${tx.account}", color = Color.Gray, fontSize = 12.sp) }
                        Text((if (tx.income) "+ " else "- ") + money(tx.amount), color = if (tx.income) Green else Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        FloatingActionButton(onClick = onAdd, modifier = Modifier.padding(18.dp).align(Alignment.Start), containerColor = Blue, contentColor = Color.White) { Icon(Icons.Default.Add, null) }
    }
}

@Composable
fun AddTransaction(onSave: (Tx) -> Unit, onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var income by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("عمومی") }
    var account by remember { mutableStateOf("حساب اصلی") }
    Column(Modifier.fillMaxSize()) {
        Header("ثبت تراکنش", back = onBack)
        Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            Text("نوع تراکنش", fontWeight = FontWeight.Bold)
            Row { FilterChip(selected = income, onClick = { income = true }, label = { Text("درآمد") }); Spacer(Modifier.width(8.dp)); FilterChip(selected = !income, onClick = { income = false }, label = { Text("هزینه") }) }
            Field("عنوان", title) { title = it }
            Field("مبلغ (ریال)", amount) { amount = it }
            Field("دسته‌بندی", category) { category = it }
            Field("حساب", account) { account = it }
            Spacer(Modifier.height(14.dp))
            Button(onClick = { amount.toLongOrNull()?.let { onSave(Tx(System.currentTimeMillis(), title.ifBlank { "تراکنش جدید" }, it, income, "۱۴۰۵/۰۶/۲۸", category, account)) } }, modifier = Modifier.fillMaxWidth(), enabled = amount.toLongOrNull() != null) { Text("ذخیره تراکنش") }
        }
    }
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), singleLine = true)
}

@Composable
fun SmsPage(onSave: (Tx) -> Unit) {
    var text by remember { mutableStateOf("") }
    var analyzed by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("ثبت از پیامک بانک")
        CardBox(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = true, onCheckedChange = {})
                Spacer(Modifier.width(8.dp))
                Column { Text("اعلان هنگام دریافت پیامک بانکی", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("متن پیامک برای یافتن مبلغ بررسی می‌شود.", color = Color.Gray, fontSize = 13.sp) }
            }
        }
        Spacer(Modifier.height(16.dp))
        CardBox(Modifier.padding(horizontal = 20.dp)) {
            Text("تست با متن پیامک", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth().height(120.dp), label = { Text("متن پیامک بانک") })
            Spacer(Modifier.height(10.dp))
            Button(onClick = { analyzed = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.AutoAwesome, null); Text("تحلیل متن پیامک") }
        }
        if (analyzed) {
            val amount = Regex("([0-9]{1,3}(?:[,،][0-9]{3})+|[0-9]{4,})").find(text)?.value?.replace(",", "")?.replace("،", "")?.toLongOrNull()
            CardBox(Modifier.padding(20.dp)) {
                Text("تراکنش پیشنهادی آماده بررسی است", fontWeight = FontWeight.Bold)
                Text(if (amount != null) "مبلغ شناسایی‌شده: ${money(amount)}" else "مبلغی شناسایی نشد.", color = Color.Gray)
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
        listOf("بودجه‌بندی هوشمند", "گزارش درآمد و هزینه", "حساب‌ها و کارت‌ها", "پشتیبان‌گیری", "تنظیمات اعلان پیامک").forEach { title ->
            CardBox(Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ChevronLeft, null, tint = Blue); Spacer(Modifier.width(8.dp)); Text(title, fontSize = 17.sp) } }
        }
        Spacer(Modifier.height(90.dp))
    }
}

fun money(value: Long): String = NumberFormat.getNumberInstance(Locale.US).format(value) + " ریال"
