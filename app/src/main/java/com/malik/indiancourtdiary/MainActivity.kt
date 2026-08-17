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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.malik.indiancourtdiary.data.CourtCase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
 val refreshing by vm.isRefreshing.collectAsStateWithLifecycle()
 var showAdd by remember{mutableStateOf(false)}
 var selected by remember{mutableStateOf<CourtCase?>(null)}
 var tab by remember{mutableIntStateOf(0)}

 if(selected!=null){CaseDetail(selected!!,vm){selected=null};return}

 Scaffold(
  topBar={TopAppBar(title={Column{Text(when(tab){0->"Court Diary";1->"Hearing Calendar";else->"Settings"});Text(when(tab){0->"Your cases, organised";1->"Today, tomorrow & upcoming";else->"Reminders & language"},style=MaterialTheme.typography.labelSmall)}})},
  bottomBar={NavigationBar{
   NavigationBarItem(tab==0,{tab=0},{Icon(Icons.Outlined.FolderOpen,null)},label={Text("My Cases")})
   NavigationBarItem(tab==1,{tab=1},{Icon(Icons.Outlined.CalendarMonth,null)},label={Text("Calendar")})
   NavigationBarItem(tab==2,{tab=2},{Icon(Icons.Outlined.Settings,null)},label={Text("Settings")})
  }},
  floatingActionButton={if(tab==0)FloatingActionButton({showAdd=true}){Icon(Icons.Outlined.Add,"Add")} }
 ){p->
  when(tab){
   0->CasesList(cases,{selected=it},{vm.delete(it)},{vm.refresh(it)},refreshing,{vm.refreshAll()},Modifier.padding(p))
   1->CalendarList(cases,{selected=it},Modifier.padding(p))
   else->SettingsScreen(vm,Modifier.padding(p))
  }
 }
 if(showAdd)AddDialog(loading,{if(!loading)showAdd=false}){cnr,reply->vm.add(cnr){e->reply(e);if(e==null)showAdd=false}}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CasesList(cases:List<CourtCase>,open:(CourtCase)->Unit,delete:(String)->Unit,refresh:(String)->Unit,refreshing:Boolean,refreshAll:()->Unit,modifier:Modifier){
 var query by remember{mutableStateOf("")}
 val filtered=remember(cases,query){if(query.isBlank())cases else cases.filter{it.cnr.contains(query,true)||it.caseTitle.contains(query,true)||it.courtName.contains(query,true)||it.clientName.contains(query,true)}}
 PullToRefreshBox(isRefreshing=refreshing,onRefresh=refreshAll,modifier=modifier){
  Column{
   OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(16.dp),label={Text("Search cases")},leadingIcon={Icon(Icons.Outlined.Search,null)},singleLine=true)
   if(filtered.isEmpty())Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text(if(cases.isEmpty())"No cases added\nTap + and enter the CNR number." else "No matching cases")}
   else LazyColumn(contentPadding=PaddingValues(horizontal=16.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    item{Text("My Cases",style=MaterialTheme.typography.headlineSmall)}
    items(filtered,key={it.cnr}){item->CaseCard(item,open,delete,refresh)}
   }
  }
 }
}

@Composable fun CaseCard(c:CourtCase,open:(CourtCase)->Unit,delete:((String)->Unit)?=null,refresh:((String)->Unit)?=null){
 ElevatedCard(Modifier.fillMaxWidth().clickable{open(c)}){
  Column(Modifier.padding(16.dp)){
   Text(c.caseTitle,style=MaterialTheme.typography.titleMedium)
   Text(c.cnr,color=MaterialTheme.colorScheme.primary)
   Text(c.courtName)
   Text("Stage: "+c.stage)
   Text(c.nextHearingDate?.let{"Next hearing: $it"}?:"Hearing date unavailable")
   Text("Updated: "+formatUpdated(c.updatedAt),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   if(delete!=null||refresh!=null)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
    if(refresh!=null)IconButton({refresh(c.cnr)}){Icon(Icons.Outlined.Refresh,"Refresh")}
    if(delete!=null)IconButton({delete(c.cnr)}){Icon(Icons.Outlined.Delete,"Delete")}
   }
  }
 }
}

fun formatUpdated(time:Long):String=DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()))

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

@Composable fun SettingsScreen(vm:CourtDiaryViewModel,modifier:Modifier){
 val context=LocalContext.current
 var days by remember{mutableIntStateOf(AppPreferences.reminderDays(context))}
 var hour by remember{mutableIntStateOf(AppPreferences.reminderHour(context))}
 var language by remember{mutableStateOf(AppPreferences.language(context))}
 var saved by remember{mutableStateOf(false)}
 Column(modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  Text("Hearing reminder",style=MaterialTheme.typography.titleLarge)
  Text("Notify me before the hearing")
  SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){
   listOf(0 to "Same day",1 to "1 day",2 to "2 days").forEachIndexed{i,item->
    SegmentedButton(selected=days==item.first,onClick={days=item.first;saved=false},shape=SegmentedButtonDefaults.itemShape(i,3)){Text(item.second)}
   }
  }
  Text("Notification time")
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   listOf(8 to "8 AM",9 to "9 AM",18 to "6 PM").forEach{item->FilterChip(selected=hour==item.first,onClick={hour=item.first;saved=false},label={Text(item.second)})}
  }
  HorizontalDivider()
  Text("Language / भाषा",style=MaterialTheme.typography.titleLarge)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   FilterChip(selected=language=="English",onClick={language="English";saved=false},label={Text("English")})
   FilterChip(selected=language=="Hindi",onClick={language="Hindi";saved=false},label={Text("हिन्दी")})
  }
  Text(if(language=="Hindi")"भाषा अगली बार ऐप खोलने पर लागू होगी।" else "Language applies when the app is opened again.",style=MaterialTheme.typography.bodySmall)
  Spacer(Modifier.weight(1f))
  Button({vm.saveSettings(days,hour,language){saved=true}},Modifier.fillMaxWidth()){Text(if(saved)"Saved" else "Save settings")}
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CaseDetail(c:CourtCase,vm:CourtDiaryViewModel,back:()->Unit){
 BackHandler(onBack=back)
 val type=object:TypeToken<List<HearingResponse>>(){}.type
 val history=remember(c.hearingHistoryJson){runCatching{Gson().fromJson<List<HearingResponse>>(c.hearingHistoryJson,type)}.getOrDefault(emptyList())}
 var name by remember{mutableStateOf(c.clientName)}
 var phone by remember{mutableStateOf(c.clientPhone)}
 var notes by remember{mutableStateOf(c.notes)}
 var saved by remember{mutableStateOf(false)}
 Scaffold(topBar={TopAppBar(title={Text("Case Details")},navigationIcon={IconButton(back){Icon(Icons.Outlined.ArrowBack,"Back")}})}){p->
  LazyColumn(Modifier.padding(p),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   item{ElevatedCard{Column(Modifier.fillMaxWidth().padding(16.dp)){Text(c.caseTitle,style=MaterialTheme.typography.titleLarge);Text(c.cnr,color=MaterialTheme.colorScheme.primary);Text(c.courtName);Text("Stage: "+c.stage);Text(c.nextHearingDate?.let{"Next hearing: $it"}?:"Next hearing unavailable")}}}
   item{Text("Client & Private Notes",style=MaterialTheme.typography.titleLarge)}
   item{OutlinedTextField(name,{name=it;saved=false},Modifier.fillMaxWidth(),label={Text("Client name")},singleLine=true)}
   item{OutlinedTextField(phone,{phone=it;saved=false},Modifier.fillMaxWidth(),label={Text("Mobile number")},singleLine=true)}
   item{OutlinedTextField(notes,{notes=it;saved=false},Modifier.fillMaxWidth(),label={Text("Private notes")},minLines=3)}
   item{Button({vm.saveMetadata(c.cnr,name,phone,notes){saved=true}},Modifier.fillMaxWidth()){Text(if(saved)"Saved" else "Save details")}}
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
