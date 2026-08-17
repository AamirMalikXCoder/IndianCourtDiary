package com.malik.indiancourtdiary
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.malik.indiancourtdiary.data.CourtCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
class CourtDiaryViewModel(a:Application):AndroidViewModel(a){
 private val dao=(a as CourtDiaryApp).database.courtCaseDao()
 val cases=dao.observeAll().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 private val _isAdding=MutableStateFlow(false);val isAdding=_isAdding.asStateFlow()
 fun add(raw:String,done:(String?)->Unit){
  val cnr=raw.trim().uppercase()
  if(!Regex("^[A-Z]{4}[0-9]{12}$").matches(cnr)){done("Enter a valid 16-character CNR");return}
  if(_isAdding.value)return
  viewModelScope.launch{
   _isAdding.value=true
   try{val x=CourtApiProvider.api.getCase(cnr);dao.save(CourtCase(cnr=x.cnr,caseTitle=x.caseTitle,courtName=x.courtName,nextHearingDate=x.nextHearingDate,stage=x.stage,hearingHistoryJson=Gson().toJson(x.hearingHistory)));done(null)}
   catch(e:HttpException){done(when(e.code()){404->"Case not found. Check the CNR number.";429->"Too many requests. Please try again later.";503->"Court service is not configured yet.";else->"Court service error. Please try again."})}
   catch(e:IOException){done("No internet connection or server unavailable.")}
   catch(e:Exception){done("Could not sync this case. Please try again.")}
   finally{_isAdding.value=false}
  }
 }
 fun delete(cnr:String)=viewModelScope.launch{dao.delete(cnr)}
}
