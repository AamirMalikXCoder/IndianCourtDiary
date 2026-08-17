package com.malik.indiancourtdiary
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.core.app.ActivityCompat
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.malik.indiancourtdiary.data.CourtCase
class MainActivity:ComponentActivity(){
 override fun onCreate(b:Bundle?){
  super.onCreate(b)
  if(Build.VERSION.SDK_INT>=33)ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.POST_NOTIFICATIONS),1001)
  setContent{MaterialTheme{Diary()}}
 }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun Diary(vm:CourtDiaryViewModel= viewModel()){
 val cases by vm.cases.collectAsStateWithLifecycle();val loading by vm.isAdding.collectAsStateWithLifecycle();var show by remember{mutableStateOf(false)};var selected by remember{mutableStateOf<CourtCase?>(null)}
 selected?.let{CaseDetail(it){selected=null};return}
 Scaffold(topBar={TopAppBar(title={Text("Court Diary")})},floatingActionButton={FloatingActionButton({show=true}){Icon(Icons.Outlined.Add,"Add")}}){p->
  if(cases.isEmpty())Box(Modifier.fillMaxSize().padding(p),contentAlignment=Alignment.Center){Text("No cases added\nTap + and enter the CNR number.")}
  else LazyColumn(Modifier.padding(p),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text("My Cases",style=MaterialTheme.typography.headlineSmall)};items(cases,key={it.cnr}){c->ElevatedCard(Modifier.fillMaxWidth().clickable{selected=c}){Column(Modifier.padding(16.dp)){Text(c.caseTitle);Text(c.cnr,color=MaterialTheme.colorScheme.primary);Text(c.courtName);Text("Stage: "+c.stage);Text(c.nextHearingDate?.let{"Next hearing: $it"}?:"Hearing date unavailable");IconButton({vm.delete(c.cnr)}){Icon(Icons.Outlined.Delete,"Delete")}}}}}
 }
 if(show)AddDialog(loading,{if(!loading)show=false}){cnr,reply->vm.add(cnr){e->reply(e);if(e==null)show=false}}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CaseDetail(c:CourtCase,back:()->Unit){
 BackHandler(onBack=back);val type=object:TypeToken<List<HearingResponse>>(){}.type
 val history=remember(c.hearingHistoryJson){runCatching{Gson().fromJson<List<HearingResponse>>(c.hearingHistoryJson,type)}.getOrDefault(emptyList())}
 Scaffold(topBar={TopAppBar(title={Text("Case Details")},navigationIcon={IconButton(back){Icon(Icons.Outlined.ArrowBack,"Back")}})}){p->
  LazyColumn(Modifier.padding(p),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   item{ElevatedCard{Column(Modifier.fillMaxWidth().padding(16.dp)){Text(c.caseTitle,style=MaterialTheme.typography.titleLarge);Text(c.cnr,color=MaterialTheme.colorScheme.primary);Text(c.courtName);Text("Stage: "+c.stage);Text(c.nextHearingDate?.let{"Next hearing: $it"}?:"Next hearing unavailable")}}}
   item{Text("Hearing History",style=MaterialTheme.typography.titleLarge)}
   if(history.isEmpty())item{Text("No hearing history available yet.")}else items(history){h->OutlinedCard{Column(Modifier.fillMaxWidth().padding(14.dp)){Text(h.date?:"Date unavailable");h.purpose?.let{Text("Purpose: $it")};h.judge?.let{Text("Judge: $it")};h.status?.let{Text("Status: $it")}}}}
  }
 }
}
@Composable fun AddDialog(loading:Boolean,close:()->Unit,save:(String,(String?)->Unit)->Unit){
 var cnr by remember{mutableStateOf("")};var error by remember{mutableStateOf<String?>(null)}
 AlertDialog(onDismissRequest=close,title={Text("Add case by CNR")},text={Column{OutlinedTextField(cnr,{cnr=it;error=null},enabled=!loading,label={Text("16-character CNR")},isError=error!=null);if(loading)LinearProgressIndicator(Modifier.fillMaxWidth());error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}},confirmButton={Button({save(cnr){error=it}},enabled=!loading){Text(if(loading)"Adding…" else "Add")}},dismissButton={TextButton(close,enabled=!loading){Text("Cancel")}})
}
