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

class CourtDiaryViewModel(private val app:Application):AndroidViewModel(app){
 private val dao=(app as CourtDiaryApp).database.courtCaseDao()
 val cases=dao.observeAll().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 private val _isAdding=MutableStateFlow(false);val isAdding=_isAdding.asStateFlow()
 private val _isRefreshing=MutableStateFlow(false);val isRefreshing=_isRefreshing.asStateFlow()

 fun add(raw:String,done:(String?)->Unit){
  val cnr=raw.trim().uppercase()
  if(!Regex("^[A-Z]{4}[0-9]{12}$").matches(cnr)){done("Enter a valid 16-character CNR");return}
  if(_isAdding.value)return
  viewModelScope.launch{
   _isAdding.value=true
   done(sync(cnr))
   _isAdding.value=false
  }
 }

 fun refresh(cnr:String,done:(String?)->Unit={}){
  if(_isRefreshing.value)return
  viewModelScope.launch{_isRefreshing.value=true;done(sync(cnr));_isRefreshing.value=false}
 }

 fun refreshAll(done:(String?)->Unit={}){
  if(_isRefreshing.value)return
  viewModelScope.launch{
   _isRefreshing.value=true
   var firstError:String?=null
   cases.value.forEach{item->val error=sync(item.cnr);if(firstError==null)firstError=error}
   done(firstError)
   _isRefreshing.value=false
  }
 }

 private suspend fun sync(cnr:String):String?{
  return try{
   val x=CourtApiProvider.api.getCase(cnr)
   val old=dao.find(cnr)
   dao.save(CourtCase(cnr=x.cnr,caseTitle=x.caseTitle,courtName=x.courtName,nextHearingDate=x.nextHearingDate,stage=x.stage,hearingHistoryJson=Gson().toJson(x.hearingHistory),clientName=old?.clientName.orEmpty(),clientPhone=old?.clientPhone.orEmpty(),notes=old?.notes.orEmpty(),isPinned=old?.isPinned?:false,isArchived=old?.isArchived?:false,updatedAt=System.currentTimeMillis()))
   HearingReminderScheduler.schedule(app,x.cnr,x.caseTitle,x.nextHearingDate)
   null
  }catch(e:HttpException){
   when(e.code()){404->"Case not found. Check the CNR number.";429->"Too many requests. Please try again later.";503->"Court service is not configured yet.";else->"Court service error. Please try again."}
  }catch(e:IOException){"No internet connection or server unavailable."}
  catch(e:Exception){"Could not sync this case. Please try again."}
 }

 fun saveMetadata(cnr:String,name:String,phone:String,notes:String,done:()->Unit)=viewModelScope.launch{
  dao.find(cnr)?.let{dao.save(it.copy(clientName=name.trim(),clientPhone=phone.trim(),notes=notes.trim()))};done()
 }
 fun togglePinned(cnr:String)=viewModelScope.launch{dao.find(cnr)?.let{dao.save(it.copy(isPinned=!it.isPinned))}}
 fun toggleArchived(cnr:String)=viewModelScope.launch{dao.find(cnr)?.let{dao.save(it.copy(isArchived=!it.isArchived))}}
 fun saveSettings(days:Int,hour:Int,language:String,done:()->Unit)=viewModelScope.launch{
  AppPreferences.save(app,days,hour,language)
  cases.value.forEach{HearingReminderScheduler.schedule(app,it.cnr,it.caseTitle,it.nextHearingDate)}
  done()
 }
 fun delete(cnr:String)=viewModelScope.launch{dao.delete(cnr);HearingReminderScheduler.cancel(app,cnr)}
}
