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

private val Blue=Color(0xFF2D66E5); private val Bg=Color(0xFFF8F9FC); private val Green=Color(0xFF14966C); private val Red=Color(0xFFD93D55)

data class Tx(val id:Long,val title:String,val amount:Long,val income:Boolean,val date:String,val category:String,val account:String)
class Db(context:Context){private val p=context.getSharedPreferences("taraz_db",0);fun load():List<Tx> = p.getString("tx","")!!.split("~").filter{it.isNotBlank()}.mapNotNull{val x=it.split("|");if(x.size<7)null else Tx(x[0].toLong(),x[1],x[2].toLong(),x[3].toBoolean(),x[4],x[5],x[6])}.sortedByDescending{it.id};fun save(x:List<Tx>)=p.edit().putString("tx",x.joinToString("~"){listOf(it.id,it.title,it.amount,it.income,it.date,it.category,it.account).joinToString("|")}).apply()}

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl){App(Db(this))}}}}

@Composable fun App(db:Db){var page by remember{mutableStateOf("home")};var txs by remember{mutableStateOf(db.load())};fun add(t:Tx){txs=listOf(t)+txs;db.save(txs)};MaterialTheme(colorScheme=lightColorScheme(primary=Blue,background=Bg)){Scaffold(containerColor=Bg,bottomBar={Nav(page){page=it}}){p->Box(Modifier.padding(p).fillMaxSize()){when(page){"home"->Home(txs){page="add"};"tx"->TxPage(txs){page="add"};"add"->Add({add(it);page="tx"}){page="home"};"sms"->Sms{add(it);page="tx"};else->More()}}}}}

@Composable fun Nav(page:String,go:(String)->Unit){NavigationBar(containerColor=Color(0xFFEAF0F6)){listOf("home" to("داشبورد مالی" to Icons.Default.Dashboard),"tx" to("تراکنش‌ها" to Icons.Default.ReceiptLong),"add" to("ثبت تراکنش" to Icons.Default.AddCircle),"sms" to("پیامک بانک" to Icons.Default.Sms),"more" to("امکانات" to Icons.Default.GridView)).forEach{(r,i)->NavigationBarItem(page==r,{go(r)},{Icon(i.second,null)},{Text(i.first,fontSize=11.sp)})}}}
@Composable fun Header(t:String,s:String?=null,back:(()->Unit)?=null){Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){if(back!=null)IconButton(back){Icon(Icons.Default.ArrowForward,null)}else Spacer(Modifier.size(48.dp));Column(horizontalAlignment=Alignment.CenterHorizontally){Text(t,fontSize=24.sp,fontWeight=FontWeight.Bold);s?.let{Text(it,color=Color.Gray,fontSize=12.sp)}};Icon(Icons.Default.Tune,null,tint=Color(0xFF63718A))}}
@Composable fun CardBox(m:Modifier=Modifier,c:@Composable ColumnScope.()->Unit){Card(m,shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,Color(0xFFE5E7EB))){Column(Modifier.padding(18.dp),content=c)}}
fun money(n:Long)=NumberFormat.getNumberInstance(Locale.US).format(n)+" ریال"

@Composable fun Home(t:List<Tx>,add:()->Unit){val inc=t.filter{it.income}.sumOf{it.amount};val exp=t.filter{!it.income}.sumOf{it.amount};Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){Header("داشبورد مالی","ابزار آنلاین برای مدیریت درآمد، هزینه، بودجه و گزارش‌ها");CardBox(Modifier.padding(horizontal=20.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.AutoGraph,null,Blue);Spacer(Modifier.width(10.dp));Column{Text("راهنمای سریع داشبورد",fontWeight=FontWeight.Bold,fontSize=18.sp);Text("مانده یعنی پولی که الان در حساب‌ها داری. درآمد وارد می‌شود و هزینه از حساب خارج می‌شود.",color=Color.Gray,fontSize=13.sp)}}};Spacer(Modifier.height(16.dp));Box(Modifier.padding(horizontal=20.dp).fillMaxWidth().background(Blue,RoundedCornerShape(18.dp)).padding(22.dp)){Column{Text("مانده فعلی",color=Color.White.copy(.8f));Text(money(inc-exp),color=Color.White,fontSize=30.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(16.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){Metric("درآمد",inc,Modifier.weight(1f));Metric("هزینه",exp,Modifier.weight(1f))};Spacer(Modifier.height(12.dp));Button(add,Modifier.align(Alignment.End)){Icon(Icons.Default.Add,null);Text("ثبت تراکنش")}}};Spacer(Modifier.height(18.dp));CardBox(Modifier.padding(horizontal=20.dp)){Text("چک‌لیست شروع",fontSize=20.sp,fontWeight=FontWeight.Bold);Text("با چند قدم ساده گزارش‌های دقیق بساز.",color=Color.Gray);listOf("ساخت اولین حساب" to Icons.Default.AccountBalanceWallet,"ثبت اولین تراکنش" to Icons.Default.ReceiptLong,"گرفتن پشتیبان" to Icons.Default.CloudUpload).forEach{Row(Modifier.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Icon(it.second,null,Blue);Spacer(Modifier.width(8.dp));Text(it.first)}}};Spacer(Modifier.height(90.dp))}}
@Composable fun Metric(t:String,v:Long,m:Modifier){Card(m,colors=CardDefaults.cardColors(Color.White.copy(.13f))){Column(Modifier.padding(12.dp)){Text(t,color=Color.White);Text(money(v),color=Color.White,fontWeight=FontWeight.Bold,fontSize=16.sp)}}}

@Composable fun TxPage(t:List<Tx>,add:()->Unit){Column(Modifier.fillMaxSize()){Header("تراکنش‌ها","ثبت و مدیریت درآمدها و هزینه‌ها");LazyColumn(Modifier.weight(1f).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(t){x->CardBox(Modifier.fillMaxWidth()){Row(verticalAlignment=Alignment.CenterVertically){Icon(if(x.income)Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,null,tint=if(x.income)Green else Red);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(x.title,fontWeight=FontWeight.Bold);Text("${x.category} • ${x.date} • ${x.account}",color=Color.Gray,fontSize=12.sp)};Text((if(x.income)"+ " else "- ")+money(x.amount),fontWeight=FontWeight.Bold,color=if(x.income)Green else Red)}}}}};FloatingActionButton(add,Modifier.padding(18.dp).align(Alignment.Start),containerColor=Blue,contentColor=Color.White){Icon(Icons.Default.Add,null)}}}

@Composable fun Add(save:(Tx)->Unit,back:()->Unit){var title by remember{mutableStateOf("")};var amount by remember{mutableStateOf("")};var income by remember{mutableStateOf(false)};var cat by remember{mutableStateOf("عمومی")};var account by remember{mutableStateOf("حساب اصلی")};Column(Modifier.fillMaxSize()){Header("ثبت تراکنش",back=back);Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())){Text("نوع تراکنش",fontWeight=FontWeight.Bold);Row{FilterChip(income,{income=true},{Text("درآمد")});Spacer(Modifier.width(8.dp));FilterChip(!income,{income=false},{Text("هزینه")})};Field("عنوان",title){title=it};Field("مبلغ (ریال)",amount){amount=it};Field("دسته‌بندی",cat){cat=it};Field("حساب",account){account=it};Button({amount.toLongOrNull()?.let{save(Tx(System.currentTimeMillis(),title.ifBlank{"تراکنش جدید"},it,income,"۱۴۰۵/۰۶/۲۸",cat,account))}},Modifier.fillMaxWidth(),enabled=amount.toLongOrNull()!=null){Text("ذخیره تراکنش")}}}}
@Composable fun Field(l:String,v:String,change:(String)->Unit)=OutlinedTextField(v,change,label={Text(l)},modifier=Modifier.fillMaxWidth().padding(vertical=6.dp),singleLine=true)

@Composable fun Sms(save:(Tx)->Unit){var text by remember{mutableStateOf("")};var analyzed by remember{mutableStateOf(false)};Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){Header("ثبت از پیامک بانک");CardBox(Modifier.padding(horizontal=20.dp)){Row(verticalAlignment=Alignment.CenterVertically){Switch(true,{});Spacer(Modifier.width(8.dp));Column{Text("اعلان هنگام دریافت پیامک بانکی",fontWeight=FontWeight.Bold,fontSize=18.sp);Text("متن پیامک برای یافتن تراکنش بررسی می‌شود.",color=Color.Gray,fontSize=13.sp)}}};Spacer(Modifier.height(16.dp));CardBox(Modifier.padding(horizontal=20.dp)){Text("تست با متن پیامک",fontSize=20.sp,fontWeight=FontWeight.Bold);OutlinedTextField(text,{text=it},Modifier.fillMaxWidth().height(120.dp));Spacer(Modifier.height(10.dp));Button({analyzed=true},Modifier.fillMaxWidth()){Icon(Icons.Default.AutoAwesome,null);Text("تحلیل متن پیامک")}};if(analyzed){val a=Regex("([0-9]{1,3}(?:[,،][0-9]{3})+|[0-9]{4,})").find(text)?.value?.replace(",","")?.replace("،","")?.toLongOrNull();CardBox(Modifier.padding(20.dp)){Text("تراکنش پیشنهادی آماده بررسی است",fontWeight=FontWeight.Bold);Text(if(a!=null)"مبلغ شناسایی‌شده: ${money(a)}" else "مبلغی شناسایی نشد.",color=Color.Gray);if(a!=null)Button({save(Tx(System.currentTimeMillis(),"تراکنش پیامکی",a,false,"۱۴۰۵/۰۶/۲۸","بانکی","حساب اصلی"))}){Text("ثبت مبلغ پیشنهادی")}}};Spacer(Modifier.height(90.dp))}}
@Composable fun More(){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){Header("امکانات");listOf("بودجه‌بندی هوشمند","گزارش درآمد و هزینه","حساب‌ها و کارت‌ها","پشتیبان‌گیری","تنظیمات اعلان پیامک").forEach{CardBox(Modifier.padding(horizontal=20.dp,vertical=5.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.ChevronLeft,null,Blue);Spacer(Modifier.width(8.dp));Text(it,fontSize=17.sp,fontWeight=FontWeight.Medium)}}};Spacer(Modifier.height(90.dp))}}
