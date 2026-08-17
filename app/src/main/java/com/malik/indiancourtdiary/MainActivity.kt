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
  setContent{CourtPremiumTheme{Diary()}}
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
 val context=LocalContext.current
 var onboarding by remember{mutableStateOf(!AppPreferences.onboardingComplete(context))}
 val hindi=AppPreferences.language(context)=="Hindi"

 if(selected!=null){CaseDetail(selected!!,vm){selected=null};return}

 Scaffold(
  containerColor=CourtNavy,
  topBar={TopAppBar(title={Column{Text(when(tab){0->if(hindi)"कोर्ट डायरी" else "Court Diary";1->if(hindi)"सुनवाई कैलेंडर" else "Hearing Calendar";else->if(hindi)"सेटिंग्स" else "Settings"});Text(when(tab){0->if(hindi)"आपके केस, व्यवस्थित" else "Your cases, organised";1->if(hindi)"आज, कल और आगामी" else "Today, tomorrow & upcoming";else->if(hindi)"रिमाइंडर और भाषा" else "Reminders & language"},style=MaterialTheme.typography.labelSmall,color=CourtMuted)}},colors=TopAppBarDefaults.topAppBarColors(containerColor=CourtNavy,titleContentColor=CourtText))},
  bottomBar={NavigationBar(containerColor=CourtSurface){
   NavigationBarItem(tab==0,{tab=0},{Icon(Icons.Outlined.FolderOpen,null)},label={Text(if(hindi)"मेरे केस" else "My Cases")})
   NavigationBarItem(tab==1,{tab=1},{Icon(Icons.Outlined.CalendarMonth,null)},label={Text(if(hindi)"कैलेंडर" else "Calendar")})
   NavigationBarItem(tab==2,{tab=2},{Icon(Icons.Outlined.Settings,null)},label={Text(if(hindi)"सेटिंग्स" else "Settings")})
  }},
  floatingActionButton={if(tab==0)FloatingActionButton({showAdd=true},containerColor=CourtGold,contentColor=CourtNavy){Icon(Icons.Outlined.Add,"Add")} }
 ){p->
  when(tab){
   0->CasesList(cases,{selected=it},{vm.delete(it)},{vm.refresh(it)},refreshing,{vm.refreshAll()},Modifier.padding(p))
   1->CalendarList(cases,{selected=it},Modifier.padding(p))
   else->SettingsScreen(vm,Modifier.padding(p))
  }
 }
 if(onboarding)OnboardingDialog{language->AppPreferences.completeOnboarding(context,language);onboarding=false}
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

@Composable fun OnboardingDialog(accept:(String)->Unit){
 var language by remember{mutableStateOf("English")}
 AlertDialog(
  onDismissRequest={},
  title={Text(if(language=="Hindi")"इंडियन कोर्ट हियरिंग डायरी" else "Indian Court Hearing Diary")},
  text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
   Text(if(language=="Hindi")"अपने केस, सुनवाई की तारीख और निजी नोट्स एक जगह रखें।" else "Track cases, hearing dates and private notes in one place.")
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    FilterChip(language=="English",{language="English"},label={Text("English")})
    FilterChip(language=="Hindi",{language="Hindi"},label={Text("हिन्दी")})
   }
   HorizontalDivider()
   Text(if(language=="Hindi")"महत्वपूर्ण सूचना" else "Important disclaimer",style=MaterialTheme.typography.titleMedium)
   Text(if(language=="Hindi")"यह सरकारी ऐप नहीं है और भारतीय न्यायपालिका या eCourts से संबद्ध नहीं है। डेटा में देरी या त्रुटि हो सकती है। अदालत में उपस्थित होने से पहले आधिकारिक cause list या संबंधित अदालत से सुनवाई की पुष्टि करें। यह कानूनी सलाह नहीं है।" else "This is not a government app and is not affiliated with the Indian judiciary or eCourts. Data may be delayed or incorrect. Confirm every hearing with the official cause list or court before attending. This app does not provide legal advice.")
  }},
  confirmButton={Button({accept(language)}){Text(if(language=="Hindi")"समझ गया, जारी रखें" else "I understand, continue")}}
 )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SettingsScreen(vm:CourtDiaryViewModel,modifier:Modifier){
 val context=LocalContext.current
 var days by remember{mutableIntStateOf(AppPreferences.reminderDays(context))}
 var hour by remember{mutableIntStateOf(AppPreferences.reminderHour(context))}
 var language by remember{mutableStateOf(AppPreferences.language(context))}
 var saved by remember{mutableStateOf(false)}
 var legalPage by remember{mutableStateOf<String?>(null)}
 if(legalPage!=null){LegalPage(legalPage!!){legalPage=null};return}
 LazyColumn(modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  item{Text("Hearing reminders",style=MaterialTheme.typography.headlineSmall,color=CourtGold)}
  item{PremiumPanel{Text("Notify me before the hearing");Spacer(Modifier.height(12.dp));SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){listOf(0 to "Same day",1 to "1 day",2 to "2 days").forEachIndexed{i,item->SegmentedButton(selected=days==item.first,onClick={days=item.first;saved=false},shape=SegmentedButtonDefaults.itemShape(i,3)){Text(item.second)}}};Spacer(Modifier.height(16.dp));Text("Notification time");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(8 to "8 AM",9 to "9 AM",18 to "6 PM").forEach{item->FilterChip(selected=hour==item.first,onClick={hour=item.first;saved=false},label={Text(item.second)})}}}}
  item{Text("Language / भाषा",style=MaterialTheme.typography.titleLarge)}
  item{PremiumPanel{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(language=="English",{language="English";saved=false},label={Text("English")});FilterChip(language=="Hindi",{language="Hindi";saved=false},label={Text("हिन्दी")})};Text(if(language=="Hindi")"अगली बार ऐप खोलने पर लागू होगा।" else "Applies when the app is opened again.",style=MaterialTheme.typography.bodySmall,color=CourtMuted)}}
  item{Button({vm.saveSettings(days,hour,language){saved=true}},Modifier.fillMaxWidth()){Text(if(saved)"Saved" else "Save settings")}}
  item{Text("Legal & support",style=MaterialTheme.typography.titleLarge)}
  items(listOf("Privacy Policy","Terms of Use","About","Contact")){page->ElevatedCard(Modifier.fillMaxWidth().clickable{legalPage=page}){Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){Icon(when(page){"Privacy Policy"->Icons.Outlined.PrivacyTip;"Terms of Use"->Icons.Outlined.Description;"About"->Icons.Outlined.Info;else->Icons.Outlined.Email},null,tint=CourtGold);Spacer(Modifier.width(14.dp));Text(page,Modifier.weight(1f));Icon(Icons.Outlined.ChevronRight,null,color=CourtMuted)}}}
 }
}

@Composable fun PremiumPanel(content:@Composable ColumnScope.()->Unit){
 ElevatedCard(Modifier.fillMaxWidth(),colors=CardDefaults.elevatedCardColors(containerColor=CourtSurfaceHigh)){Column(Modifier.fillMaxWidth().padding(18.dp),content=content)}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun LegalPage(page:String,back:()->Unit){
 BackHandler(onBack=back)
 val body=when(page){
  "Privacy Policy"->"Your saved cases, client names, phone numbers and private notes remain in the app's local database. CNR numbers are sent to court.reports.ink only when you add or refresh a case. The server uses them to retrieve court information and may temporarily cache court responses. We do not sell personal data. Do not enter confidential information that is not required."
  "Terms of Use"->"This is an independent, non-government case diary. It is not affiliated with the Indian judiciary, eCourts or any court. Court data can be delayed, incomplete or incorrect. Always verify hearings, courtroom and cause-list position through official sources. The app is not legal advice and reminders are convenience alerts only."
  "About"->"Indian Court Hearing Diary helps litigants and advocates organise CNR-based cases, hearing dates, history, reminders and private notes. App version 1.0.0."
  else->"Support website: https://court.reports.ink\n\nWhen contacting support, describe the issue and app version. Never send passwords, OTPs, payment details or confidential legal documents."
 }
 Scaffold(containerColor=CourtNavy,topBar={TopAppBar(title={Text(page)},navigationIcon={IconButton(back){Icon(Icons.Outlined.ArrowBack,"Back")}},colors=TopAppBarDefaults.topAppBarColors(containerColor=CourtNavy))}){p->Column(Modifier.padding(p).padding(20.dp)){PremiumPanel{Text(body,style=MaterialTheme.typography.bodyLarge);if(page=="Privacy Policy"||page=="Terms of Use"){Spacer(Modifier.height(16.dp));Text("Last updated: 18 August 2026",style=MaterialTheme.typography.labelMedium,color=CourtGold)}}}}
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
