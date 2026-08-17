package com.malik.indiancourtdiary
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
class MainActivity:ComponentActivity(){
 override fun onCreate(b:Bundle?){super.onCreate(b);setContent{MaterialTheme{Diary()}}}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun Diary(vm:CourtDiaryViewModel= viewModel()){
 val cases by vm.cases.collectAsStateWithLifecycle();var show by remember{mutableStateOf(false)}
 Scaffold(topBar={TopAppBar(title={Column{Text("Court Diary");Text("Your hearings, organised",style=MaterialTheme.typography.labelSmall)}})},floatingActionButton={FloatingActionButton({show=true}){Icon(Icons.Outlined.Add,"Add case")}}){p->
  if(cases.isEmpty())Box(Modifier.fillMaxSize().padding(p),contentAlignment=Alignment.Center){Text("No cases added\nTap + and enter the CNR number.")}
  else LazyColumn(Modifier.padding(p),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   item{Text("My Cases",style=MaterialTheme.typography.headlineSmall)}
   items(cases,key={it.cnr}){c->ElevatedCard{Column(Modifier.fillMaxWidth().padding(16.dp)){Text(c.caseTitle);Text(c.cnr,color=MaterialTheme.colorScheme.primary);Text(c.courtName);Text(c.nextHearingDate?:"Hearing date awaiting sync");IconButton({vm.delete(c.cnr)}){Icon(Icons.Outlined.Delete,"Delete")}}}}
  }
 }
 if(show)AddDialog({show=false}){cnr,reply->vm.add(cnr){e->reply(e);if(e==null)show=false}}
}
@Composable fun AddDialog(close:()->Unit,save:(String,(String?)->Unit)->Unit){
 var cnr by remember{mutableStateOf("")};var error by remember{mutableStateOf<String?>(null)}
 AlertDialog(onDismissRequest=close,title={Text("Add case by CNR")},text={Column{OutlinedTextField(cnr,{cnr=it;error=null},label={Text("16-character CNR")},isError=error!=null);error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}},confirmButton={Button({save(cnr){error=it}}){Text("Add")}},dismissButton={TextButton(close){Text("Cancel")}})
}
