package com.malik.indiancourtdiary
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.malik.indiancourtdiary.data.CourtCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
class CourtDiaryViewModel(a:Application):AndroidViewModel(a){
 private val dao=(a as CourtDiaryApp).database.courtCaseDao()
 val cases=dao.observeAll().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 fun add(raw:String,done:(String?)->Unit){val cnr=raw.trim().uppercase();if(!Regex("^[A-Z]{4}[0-9]{12}$").matches(cnr))return done("Enter a valid 16-character CNR");viewModelScope.launch{dao.save(CourtCase(cnr=cnr));done(null)}}
 fun delete(cnr:String)=viewModelScope.launch{dao.delete(cnr)}
}
