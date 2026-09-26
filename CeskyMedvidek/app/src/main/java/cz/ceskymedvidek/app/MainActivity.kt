package cz.ceskymedvidek.app

import android.app.*
import android.os.*
import android.speech.tts.TextToSpeech
import android.graphics.Color
import android.graphics.Typeface
import android.view.*
import android.widget.*
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var root: LinearLayout
    private lateinit var tts: TextToSpeech
    private var stars = 0
    private val words = listOf("kráva" to "🐮", "pes" to "🐶", "kočka" to "🐱", "kůň" to "🐴", "prase" to "🐷", "kuře" to "🐔", "ovce" to "🐑", "kachna" to "🦆", "králík" to "🐰", "strom" to "🌳", "jablko" to "🍎", "banán" to "🍌", "mléko" to "🥛", "voda" to "💧", "chleba" to "🍞", "auto" to "🚗", "autobus" to "🚌", "kolo" to "🚲", "míč" to "⚽", "dům" to "🏠")

    override fun onCreate(b: Bundle?) { super.onCreate(b); tts=TextToSpeech(this,this); home() }
    override fun onInit(s:Int){ if(s==TextToSpeech.SUCCESS) tts.language=Locale("cs","CZ") }
    private fun base(title:String){ root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(28,48,28,28);setBackgroundColor(Color.rgb(234,247,255))}; setContentView(root); text(title,30,true) }
    private fun text(s:String,size:Int=22,bold:Boolean=false){ root.addView(TextView(this).apply{text=s;textSize=size.toFloat();gravity=Gravity.CENTER;setTextColor(Color.rgb(25,63,112));if(bold)setTypeface(typeface,Typeface.BOLD);setPadding(10,14,10,14)}) }
    private fun btn(s:String, action:()->Unit){ root.addView(Button(this).apply{text=s;textSize=20f;setAllCaps(false);setPadding(16,10,16,10);setOnClickListener{action()}} , LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,10,12,10)}) }
    private fun say(s:String){tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"cz")}
    private fun home(){ base("Český medvídek"); text("🧸",92); text("Ahoj! Pojď si hrát a učit se česky.",20); btn("🐄  Farma – učíme se slova"){learn(0)}; btn("🎯  Najdi správný obrázek"){quiz()}; btn("⭐  Moje hvězdičky: $stars"){reward()} }
    private fun learn(i:Int){ val x=words[i%words.size]; base("Farma"); text(x.second,110); text(x.first.uppercase(),34,true); btn("🔊 Poslechni si: ${x.first}"){say(x.first)}; btn("Další ➜"){learn(i+1)}; btn("⌂ Domů"){home()} }
    private fun quiz(){ val target=words.random(); val choices=(words.shuffled().filter{it!=target}.take(2)+target).shuffled(); base("Najdi: ${target.first}"); btn("🔊 Přehrát zadání"){say("Najdi ${target.first}")}; choices.forEach { c -> btn("${c.second}   ${c.first}"){ if(c==target){stars++; say("Výborně"); Toast.makeText(this,"⭐ Výborně!",Toast.LENGTH_SHORT).show(); quiz()} else {say("Zkus to ještě jednou"); Toast.makeText(this,"Zkus to ještě jednou",Toast.LENGTH_SHORT).show()} } }; btn("⌂ Domů"){home()} }
    private fun reward(){base("Moje odměny");text("🧸",90);text("⭐ $stars",48,true);text(if(stars<5)"Získej 5 hvězdiček!" else "Výborně! Medvídek má radost!",22);btn("Hrát dál"){quiz()};btn("⌂ Domů"){home()} }
    override fun onDestroy(){tts.stop();tts.shutdown();super.onDestroy()}
}
