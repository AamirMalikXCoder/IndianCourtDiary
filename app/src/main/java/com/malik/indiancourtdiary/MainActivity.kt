package com.malik.indiancourtdiary
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.malik.indiancourtdiary.data.CourtCase
import java.time.LocalDate

class MainActivity:ComponentActivity(){
 override fun onCreate(b:Bundle?){
  super.onCreate(b)
  if(Build.VERSION.SDK_INT>=33)ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.POST_NOTIFICATIONS),1001)
  setContent{MaterialTheme{Diary()}}
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun Diary(vm:CourtDiaryViewModel= viewModel()){
 val cases by vm.cases.collectAsStateWithLifecycle()
 val loading by vm.isAdding.collectAsStateWithLifecycle()
 var showAdd by remember{mutableStateOf(false)}
 var selected by remember{mutableStateOf<CourtCase?>(null)}
 var tab by remember{mutableIntStateOf(0)}

 if(selected!=null){CaseDetail(selected!!){selected=null};return}

 Scaffold(
  topBar={TopAppBar(title={Column{Text(if(tab==0)"Court Diary" else "Hearing Calendar");Text(if(tab==0)"Your cases, organised" else "Today, tomorrow & upcoming",style=MaterialTheme.typography.labelSmall)}})},
  bottomBar={NavigationBar{
   NavigationBarItem(tab==0,{tab=0},{Icon(Icons.Outlined.FolderOpen,null)},label={Text("My Cases")})
   NavigationBarItem(tab==1,{tab=1},{Icon(Icons.Outlined.CalendarMonth,null)},label={Text("Calendar")})
  }},
  floatingActionButton={if(tab==0)FloatingActionButton({showAdd=true}){Icon(Icons.Outlined.Add,"Add")} }
 ){p->
  if(tab==0)CasesList(cases,{selected=it},{vm.delete(it)},Modifier.padding(p))
  else CalendarList(cases,{selected=it},Modifier.padding(p))
 }
 if(showAdd)AddDialog(loading,{if(!loading)showAdd=false}){cnr,reply->vm.add(cnr){e->reply(e);if(e==null)showAdd=false}}
}

@Composable fun CasesList(cases:List<CourtCase>,open:(CourtCase)->Unit,delete:(String)->Unit,modifier:Modifier){
 if(cases.isEmpty())Box(modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("No cases added\nTap + and enter the CNR number.")}
 else LazyColumn(modifier,contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item{Text("My Cases",style=MaterialTheme.typography.headlineSmall)}
  items(cases,key={it.cnr}){c->CaseCard(c,open,delete)}
 }
}

@Composable fun CaseCard(c:CourtCase,open:(CourtCase)->Unit,delete:((String)->Unit)?=null){
 ElevatedCard(Modifier.fillMaxWidth().clickable{open(c)}){
  Column(Modifier.padding(16.dp)){
   Text(c.caseTitle,style=MaterialTheme.typography.titleMedium)
   Text(c.cnr,color=MaterialTheme.colorScheme.primary)
   Text(c.courtName)
   Text("Stage: "+c.stage)
   Text(c.nextHearingDate?.let{"Next hearing: $it"}?:"Hearing date unavailable")
   if(delete!=null)IconButton({delete(c.cnr)}){Icon(Icons.Outlined.Delete,"Delete")}
  }
 }
}

@Composable fun CalendarList(cases:List<CourtCase>,open:(CourtCase)->Unit,modifier:Modifier){
 val today=LocalDate.now()
 val dated=cases.mapNotNull{c->runCatching{LocalDate.parse(c.nextHearingDate?.take(10))}.getOrNull()?.let{it to c}}.sortedBy{it.first}
 val todayCases=dated.filter{it.first==today}.map{it.second}
 val tomorrowCases=dated.filter{it.first==today.plusDays(1)}.map{it.second}
 val upcoming=dated.filter{it.first>today.plusDays(1)}.map{it.second}
 LazyColumn(modifier,contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  hearingSection("Today",todayCases,open)
  hearingSection("Tomorrow",tomorrowCases,open)
  hearingSection("Upcoming",upcoming,open)
  if(dated.isEmpty())item{Text("No upcoming hearing dates available.")}
 }
}

fun androidx.compose.foundation.lazy.LazyListScope.hearingSection(title:String,list:List<CourtCase>,open:(CourtCase)->Unit){
 item{Text(title,style=MaterialTheme.typography.titleLarge)}
 if(list.isEmpty())item{Text("No hearings",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
 else items(list,key={title+it.cnr}){CaseCard(it,open)}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CaseDetail(c:CourtCase,back:()->Unit){
 BackHandler(onBack=back)
 val type=object:TypeToken<List<HearingResponse>>(){}.type
 val history=remember(c.hearingHistoryJson){runCatching{Gson().fromJson<List<HearingResponse>>(c.hearingHistoryJson,type)}.getOrDefault(emptyList())}
 Scaffold(topBar={TopAppBar(title={Text("Case Details")},navigationIcon={IconButton(back){Icon(Icons.Outlined.ArrowBack,"Back")}})}){p->
  LazyColumn(Modifier.padding(p),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   item{ElevatedCard{Column(Modifier.fillMaxWidth().padding(16.dp)){Text(c.caseTitle,style=MaterialTheme.typography.titleLarge);Text(c.cnr,color=MaterialTheme.colorScheme.primary);Text(c.courtName);Text("Stage: "+c.stage);Text(c.nextHearingDate?.let{"Next hearing: $it"}?:"Next hearing unavailable")}}}
   item{Text("Hearing History",style=MaterialTheme.typography.titleLarge)}
   if(history.isEmpty())item{Text("No hearing history available yet.")}
   else items(history){h->OutlinedCard{Column(Modifier.fillMaxWidth().padding(14.dp)){Text(h.date?:"Date unavailable");h.purpose?.let{Text("Purpose: $it")};h.judge?.let{Text("Judge: $it")};h.status?.let{Text("Status: $it")}}}}
  }
 }
}

@Composable fun AddDialog(loading:Boolean,close:()->Unit,save:(String,(String?)->Unit)->Unit){
 var cnr by remember{mutableStateOf("")};var error by remember{mutableStateOf<String?>(null)}
 AlertDialog(onDismissRequest=close,title={Text("Add case by CNR")},text={Column{
  OutlinedTextField(cnr,{cnr=it;error=null},enabled=!loading,label={Text("16-character CNR")},isError=error!=null,singleLine=true)
  if(loading){Spacer(Modifier.height(10.dp));LinearProgressIndicator(Modifier.fillMaxWidth());Text("Fetching case details…")}
  error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
 }},confirmButton={Button({save(cnr){error=it}},enabled=!loading){Text(if(loading)"Adding…" else "Add")}},dismissButton={TextButton(close,enabled=!loading){Text("Cancel")}})
}
