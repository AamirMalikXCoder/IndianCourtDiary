package com.malik.indiancourtdiary

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.malik.indiancourtdiary.data.CourtCase
import java.io.File

object CaseExporter {
 fun shareText(context:Context,item:CourtCase){
  val text=buildString{
   appendLine("⚖️ Indian Court Hearing Diary");appendLine();appendLine(item.caseTitle)
   appendLine("CNR: "+item.cnr);appendLine("Court: "+item.courtName);appendLine("Stage: "+item.stage)
   appendLine("Next hearing: "+(item.nextHearingDate?:"Not available"))
   if(item.clientName.isNotBlank())appendLine("Client: "+item.clientName)
   appendLine();append("Please verify the hearing through the official court/cause list.")
  }
  val intent=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_SUBJECT,"Court case "+item.cnr);putExtra(Intent.EXTRA_TEXT,text)}
  context.startActivity(Intent.createChooser(intent,"Share case summary"))
 }

 fun sharePdf(context:Context,item:CourtCase){
  val directory=File(context.cacheDir,"shared").apply{mkdirs()};val file=File(directory,"case-"+item.cnr+".pdf")
  val document=PdfDocument();val page=document.startPage(PdfDocument.PageInfo.Builder(595,842,1).create());val canvas=page.canvas
  val title=Paint().apply{color=Color.rgb(30,42,58);textSize=22f;isFakeBoldText=true}
  val gold=Paint().apply{color=Color.rgb(165,125,45);textSize=12f;isFakeBoldText=true}
  val body=Paint().apply{color=Color.rgb(45,50,58);textSize=11f}
  val muted=Paint().apply{color=Color.rgb(95,100,108);textSize=9f}
  canvas.drawColor(Color.WHITE);canvas.drawRect(0f,0f,595f,12f,Paint().apply{color=Color.rgb(215,181,109)})
  canvas.drawText("INDIAN COURT HEARING DIARY",42f,58f,title);canvas.drawText("CASE SUMMARY",42f,82f,gold)
  var y=120f
  fun row(label:String,value:String){canvas.drawText(label.uppercase(),42f,y,gold);y+=18f;value.chunked(78).forEach{line->canvas.drawText(line,42f,y,body);y+=16f};y+=8f}
  row("Case",item.caseTitle);row("CNR",item.cnr);row("Court",item.courtName);row("Current stage",item.stage);row("Next hearing",item.nextHearingDate?:"Not available")
  if(item.clientName.isNotBlank())row("Client",item.clientName);if(item.clientPhone.isNotBlank())row("Mobile",item.clientPhone);if(item.notes.isNotBlank())row("Private notes",item.notes)
  val historyType=(object:TypeToken<List<HearingResponse>>(){}).type
  val history=runCatching{Gson().fromJson<List<HearingResponse>>(item.hearingHistoryJson,historyType)}.getOrDefault(emptyList())
  if(history.isNotEmpty()&&y<680f){canvas.drawText("RECENT HEARINGS",42f,y,gold);y+=22f;history.take(8).forEach{h->val line=listOfNotNull(h.date,h.purpose,h.status).joinToString(" • ");line.chunked(82).forEach{part->if(y<755f){canvas.drawText(part,42f,y,body);y+=15f}};y+=5f}}
  canvas.drawLine(42f,780f,553f,780f,Paint().apply{color=Color.LTGRAY});canvas.drawText("Independent non-government diary. Verify all hearings with official court sources.",42f,800f,muted)
  document.finishPage(page);file.outputStream().use{document.writeTo(it)};document.close()
  val uri=FileProvider.getUriForFile(context,context.packageName+".fileprovider",file)
  val intent=Intent(Intent.ACTION_SEND).apply{type="application/pdf";putExtra(Intent.EXTRA_STREAM,uri);putExtra(Intent.EXTRA_SUBJECT,"Court case "+item.cnr);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)}
  context.startActivity(Intent.createChooser(intent,"Share case PDF"))
 }
}
