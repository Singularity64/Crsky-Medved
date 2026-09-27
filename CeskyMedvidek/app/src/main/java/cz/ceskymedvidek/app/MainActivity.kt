package cz.ceskymedvidek.app
import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.*
import java.text.Normalizer
import java.util.Locale

data class Word(val name:String,val icon:String,val world:String)
data class StickerReward(val key:String,val icon:String,val name:String)
data class RewardGrant(val praise:String,val reward:StickerReward,val isNew:Boolean,val collectionCompleted:Boolean)

class MainActivity:Activity(),TextToSpeech.OnInitListener{
 companion object{private const val REQ_RECORD_AUDIO=41}
 private lateinit var root:LinearLayout
 private lateinit var tts:TextToSpeech
 private var speechRecognizer:SpeechRecognizer?=null
 private var speechStatus:TextView?=null
 private var speechNextButton:Button?=null
 private var expectedSpeechWord:String?=null
 private var expectedSpeechWorld:String?=null
 private var speechWordPassed=false
 private var speechScreenActive=false
 private var resumeListeningAfterTts:String?=null
 private val speechHandler=Handler(Looper.getMainLooper())
 private val prefs by lazy{getSharedPreferences("medvidek",MODE_PRIVATE)}
 private var stars:Int get()=prefs.getInt("stars",0); set(v){prefs.edit().putInt("stars",v).apply()}
 private val praisePhrases=listOf(
  "Výborně!",
  "Paráda!",
  "Skvěle!",
  "Perfektní!",
  "To se ti povedlo!",
  "Jsi šikula!",
  "Super práce!",
  "Máš to!",
  "Nádhera!",
  "Jen tak dál!"
 )
 private val stickerRewards=listOf(
  StickerReward("beri","🧸","Beri"),
  StickerReward("balonek","🎈","Balónek"),
  StickerReward("hvezda","⭐","Hvězdička"),
  StickerReward("duha","🌈","Duha"),
  StickerReward("srdce","💖","Srdíčko"),
  StickerReward("korunka","👑","Korunka"),
  StickerReward("pohar","🏆","Pohár"),
  StickerReward("raketa","🚀","Raketa"),
  StickerReward("jednorozec","🦄","Jednorožec"),
  StickerReward("darek","🎁","Dárek")
 )
 private val vocabulary=mapOf(
 "Domeček" to listOf("dům","panelák","pokoj","postel","polštář","deka","stůl","židle","okno","dveře"),
 "Domácnost" to listOf("kuchyně","koupelna","ložnice","chodba","balkon","zahrada","garáž","střecha","zeď","podlaha","strop","schody","klíč","zámek","křeslo","police","koberec","zrcadlo","hodiny","obraz","záclona","peřina","ručník","mýdlo","kartáček","hřeben","vysavač","koště","lopatka","pračka","lednice","trouba","sporák","konvice","hrnek","talíř","lžíce"),
 "Zvířata" to listOf("pes","kočka","králík","křeček","morče","papoušek","ryba","želva","kůň","kráva","prase","ovce","koza","slepice","kohout","kachna","husa","krocan","osel","jelen","srna","liška","vlk","medvěd","ježek","veverka","zajíc","divočák","myš","krtek","lev","tygr","slon","žirafa","zebra","opice","gorila","klokan","panda","hroch","nosorožec","krokodýl","had","ještěrka","žába","čáp","sova","orel","tučňák","delfín"),
 "Jídlo" to listOf("chléb","rohlík","houska","máslo","sýr","šunka","salám","vejce","mléko","jogurt","tvaroh","smetana","polévka","maso","kuře","ryba","rýže","těstoviny","brambory","knedlík","omáčka","salát","pizza","hamburger","párek","kaše","mouka","cukr","sůl","pepř","med","džem","čokoláda","sušenka","dort","zmrzlina","bonbón","ořech","snídaně","oběd","večeře","svačina","voda","čaj","kakao","džus","limonáda","hlad","žízeň","chuť"),
 "Ovoce a zelenina" to listOf("jablko","hruška","banán","pomeranč","mandarinka","citron","limetka","grep","broskev","meruňka","švestka","třešeň","višeň","jahoda","malina","ostružina","borůvka","rybíz","angrešt","hroznové víno","meloun","ananas","mango","kiwi","kokos","avokádo","rajče","okurka","paprika","mrkev","petržel","celer","ředkvička","řepa","zelí","kapusta","salát","špenát","brokolice","květák","hrášek","fazole","čočka","kukuřice","cibule","česnek","dýně","cuketa","lilek","houba"),
 "Tělo" to listOf("hlava","vlasy","čelo","oko","obočí","řasa","ucho","nos","tvář","pusa","ret","zub","jazyk","brada","krk","rameno","ruka","loket","zápěstí","dlaň","prst","palec","nehet","hrudník","břicho","záda","bok","noha","koleno","kotník","pata","chodidlo","kůže","kost","sval","srdce","mozek","krev","dech","hlas","slza","pot","úsměv","bolest","zdraví","nemoc","teplota","kašel","rýma","spánek"),
 "Oblečení" to listOf("tričko","košile","halenka","mikina","svetr","bunda","kabát","vesta","kalhoty","džíny","kraťasy","sukně","šaty","pyžamo","spodní prádlo","ponožka","punčocha","boty","tenisky","sandály","holínky","bačkory","čepice","klobouk","kšiltovka","šála","rukavice","pásek","kravata","motýlek","kapsa","knoflík","zip","kapuce","deštník","batoh","kabelka","kufr","brýle","sluneční brýle","hodinky","prsten","náhrdelník","náramek","náušnice","velikost","barva","látka","vlna","bavlna"),
 "Doprava" to listOf("auto","autobus","trolejbus","tramvaj","vlak","metro","taxi","kolo","koloběžka","motorka","nákladní auto","dodávka","traktor","sanitka","policejní auto","hasičské auto","loď","člun","trajekt","letadlo","vrtulník","raketa","silnice","ulice","dálnice","most","tunel","křižovatka","semafor","přechod","chodník","zastávka","nádraží","letiště","přístav","parkoviště","garáž","volant","kolo auta","pneumatika","motor","sedadlo","pás","jízdenka","lístek","řidič","cestující","cesta","výlet","dopravní značka"),
 "Město" to listOf("město","vesnice","dům","panelák","škola","školka","nemocnice","lékárna","obchod","supermarket","pekárna","restaurace","kavárna","pošta","banka","knihovna","kino","divadlo","muzeum","kostel","radnice","policie","hasiči","park","hřiště","stadion","bazén","zoo","náměstí","ulice","chodník","lavička","kašna","socha","věž","most","řeka","semafor","přechod","zastávka","nádraží","parkoviště","popelnice","kontejner","výloha","vchod","výtah","eskalátor","mapa","adresa"),
 "Příroda" to listOf("strom","keř","tráva","květina","list","větev","kořen","kmen","kůra","semínko","plod","les","louka","pole","zahrada","park","hora","kopec","skála","jeskyně","údolí","řeka","potok","jezero","rybník","moře","oceán","ostrov","pláž","písek","kámen","hlína","bahno","voda","vodopád","pramen","obloha","slunce","měsíc","hvězda","mrak","déšť","sníh","led","vítr","bouřka","blesk","duha","stín","příroda"),
 "Škola" to listOf("škola","třída","učitel","učitelka","žák","žákyně","kamarád","lavice","tabule","křída","fix","sešit","učebnice","kniha","papír","tužka","pero","pastelka","guma","pravítko","nůžky","lepidlo","aktovka","penál","úkol","otázka","odpověď","písmeno","slovo","věta","číslo","počítání","čtení","psaní","kreslení","zpěv","hudba","tělocvik","přestávka","zvonění","oběd","jídelna","družina","výlet","známka","pochvala","chyba","nápad","učení","prázdniny"),
 "Hračky" to listOf("hračka","panenka","medvídek","míč","kostka","stavebnice","puzzle","pexeso","autíčko","vláček","letadélko","loďka","robot","drak","balónek","bublifuk","houpačka","skluzavka","pískoviště","koloběžka","tříkolka","kolo","brusle","lyže","sáňky","pastelky","omalovánky","plastelína","loutka","maňásek","domeček","garáž","dráha","figurky","karty","kostky","hra","schovávaná","honička","skákání","stavění","kreslení","malování","zpívání","tanec","pohádka","dárek","oslava","narozeniny","zábava"),
 "Barvy a tvary" to listOf("červená","modrá","zelená","žlutá","oranžová","fialová","růžová","hnědá","černá","bílá","šedá","zlatá","stříbrná","světlá","tmavá","barevná","kruh","čtverec","trojúhelník","obdélník","ovál","hvězda","srdce","kříž","čára","bod","roh","strana","koule","kostka","válec","kužel","malý","velký","dlouhý","krátký","široký","úzký","vysoký","nízký","kulatý","hranatý","rovný","křivý","plný","prázdný","stejný","jiný","uprostřed","okraj"),
 "Čísla a čas" to listOf("nula","jedna","dva","tři","čtyři","pět","šest","sedm","osm","devět","deset","jedenáct","dvanáct","třináct","čtrnáct","patnáct","šestnáct","sedmnáct","osmnáct","devatenáct","dvacet","první","druhý","třetí","poslední","málo","hodně","více","méně","všechno","nic","čas","hodina","minuta","sekunda","den","noc","ráno","dopoledne","poledne","odpoledne","večer","dnes","včera","zítra","týden","měsíc","rok","brzy","pozdě"),
 "Rodina a lidé" to listOf("máma","tatínek","rodič","dítě","syn","dcera","bratr","sestra","babička","dědeček","vnuk","vnučka","teta","strýc","bratranec","sestřenice","rodina","kamarád","kamarádka","soused","sousedka","miminko","kluk","holka","muž","žena","pán","paní","člověk","jméno","věk","domov","návštěva","svatba","narozeniny","pomoc","pozdrav","ahoj","dobrý den","prosím","děkuji","promiň","ano","ne","spolu","sám","radost","smutek","láska","přátelství"),
 "Povolání" to listOf("lékař","lékařka","zdravotní sestra","zubař","učitel","učitelka","policista","policistka","hasič","hasička","řidič","řidička","kuchař","kuchařka","pekař","pekařka","prodavač","prodavačka","pošťák","farmář","zahradník","mechanik","stavitel","zedník","truhlář","malíř","uklízeč","kadeřník","fotograf","herec","herečka","zpěvák","zpěvačka","hudebník","sportovec","trenér","pilot","strojvedoucí","průvodčí","kapitán","vědec","programátor","veterinář","záchranář","knihovník","číšník","servírka","řezník","cukrář","práce"),
 "Sport" to listOf("sport","fotbal","hokej","tenis","basketbal","volejbal","házená","florbal","baseball","golf","plavání","běh","skok","cyklistika","lyžování","bruslení","gymnastika","tanec","judo","karate","box","míč","branka","gól","hřiště","stadion","bazén","raketa","síť","hokejka","puk","helma","dres","trenýrky","tenisky","kopačky","medaile","pohár","závod","tým","hráč","trenér","rozhodčí","výhra","prohra","remíza","start","cíl","rychlost","síla"),
 "Slovesa" to listOf("být","mít","dělat","jít","jet","běžet","chodit","skákat","sedět","stát","ležet","spát","vstát","jíst","pít","vařit","péct","krájet","míchat","umýt","čistit","uklízet","otevřít","zavřít","dát","vzít","nést","držet","pustit","hodit","chytit","kopnout","vidět","dívat se","slyšet","poslouchat","mluvit","říct","ptát se","odpovědět","číst","psát","kreslit","počítat","učit se","hrát si","zpívat","tančit","smát se","plakat"),
 "Vlastnosti" to listOf("dobrý","špatný","hezký","ošklivý","nový","starý","mladý","rychlý","pomalý","silný","slabý","lehký","těžký","teplý","studený","horký","čistý","špinavý","suchý","mokrý","měkký","tvrdý","hladký","drsný","sladký","slaný","kyselý","hořký","hlasitý","tichý","veselý","smutný","hodný","zlý","chytrý","hloupý","statečný","opatrný","unavený","zdravý","nemocný","hladový","žíznivý","plný","prázdný","otevřený","zavřený","blízko","daleko","společný"),
 "Počasí a roční doby" to listOf("počasí","jaro","léto","podzim","zima","leden","únor","březen","duben","květen","červen","červenec","srpen","září","říjen","listopad","prosinec","slunce","mrak","déšť","kapka","sníh","vločka","led","kroupy","vítr","vánek","bouřka","blesk","hrom","duha","mlha","mráz","teplo","horko","chlad","stín","světlo","tma","obloha","teplota","deštník","pláštěnka","kaluž","sněhulák","sáňky","rukavice","čepice","prázdniny","roční období"),
 "Výlet a cestování" to listOf("výlet","cesta","dovolená","batoh","kufr","taška","mapa","kompas","stan","spacák","karimatka","baterka","láhev","svačina","fotoaparát","fotka","jízdenka","letenka","pas","hotel","pokoj","recepce","kemp","chata","hrad","zámek","rozhledna","muzeum","zoo","hora","les","jezero","moře","pláž","ostrov","letiště","nádraží","přístav","vlak","autobus","letadlo","loď","turista","průvodce","směr","vlevo","vpravo","rovně","návrat","zážitek")
 )
 private val worldIcons=mapOf(
 "Domeček" to "🏠",
 "Domácnost" to "🏡",
 "Zvířata" to "🐻",
 "Jídlo" to "🍽️",
 "Ovoce a zelenina" to "🍎",
 "Tělo" to "🧒",
 "Oblečení" to "👕",
 "Doprava" to "🚗",
 "Město" to "🏙️",
 "Příroda" to "🌳",
 "Škola" to "🎒",
 "Hračky" to "🧸",
 "Barvy a tvary" to "🎨",
 "Čísla a čas" to "🔢",
 "Rodina a lidé" to "👨‍👩‍👧",
 "Povolání" to "👷",
 "Sport" to "⚽",
 "Slovesa" to "🏃",
 "Vlastnosti" to "✨",
 "Počasí a roční doby" to "🌦️",
 "Výlet a cestování" to "🗺️"
 )
 private val words by lazy { vocabulary.flatMap { (world,names) -> names.map { Word(it, worldIcons[world] ?: "⭐", world) } } }
 private val worldList by lazy { vocabulary.keys.map { it to (worldIcons[it] ?: "⭐") } }
 override fun onCreate(b:Bundle?){super.onCreate(b);tts=TextToSpeech(this,this);splash()}
 override fun onInit(s:Int){
  if(s==TextToSpeech.SUCCESS){
   tts.language=Locale("cs","CZ")
   tts.setSpeechRate(.82f)
   tts.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
    override fun onStart(utteranceId:String?){}
    override fun onDone(utteranceId:String?){
     if(utteranceId=="word_playback"){
      val word=resumeListeningAfterTts
      resumeListeningAfterTts=null
      if(word!=null)runOnUiThread{
       if(speechScreenActive&&!speechWordPassed&&expectedSpeechWord==word)startListening(word)
      }
     }
    }
    override fun onError(utteranceId:String?){
     if(utteranceId=="word_playback"){
      val word=resumeListeningAfterTts
      resumeListeningAfterTts=null
      if(word!=null)runOnUiThread{
       if(speechScreenActive&&!speechWordPassed&&expectedSpeechWord==word)startListening(word)
      }
     }
    }
   })
  }
 }
 private fun base(){
  speechScreenActive=false
  resumeListeningAfterTts=null
  cancelListening()
  root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(28,42,28,28);background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(220,246,255),Color.rgb(247,238,255)))}
  setContentView(ScrollView(this).apply{addView(root)})
 }
 private fun text(s:String,size:Int=22,bold:Boolean=false){root.addView(TextView(this).apply{text=s;textSize=size.toFloat();gravity=Gravity.CENTER;setTextColor(Color.rgb(31,61,112));if(bold)setTypeface(typeface,Typeface.BOLD);setPadding(8,10,8,10)})}
 private fun speechInfo(s:String="🎤 Mikrofon je zapnutý. Řekni slovo."){
  speechStatus=TextView(this).apply{text=s;textSize=17f;gravity=Gravity.CENTER;setTextColor(Color.rgb(31,61,112));setPadding(8,8,8,12)}
  root.addView(speechStatus)
 }
 private fun bigBtn(title:String,subtitle:String,color:Int,a:()->Unit){
  root.addView(Button(this).apply{
   text=title+"\n"+subtitle
   textSize=22f
   setAllCaps(false)
   gravity=Gravity.CENTER
   setTextColor(Color.WHITE)
   setTypeface(typeface,Typeface.BOLD)
   background=GradientDrawable().apply{setColor(color);cornerRadius=42f}
   setPadding(18,28,18,28)
   minHeight=150
   setOnClickListener{a()}
  },LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,12,8,12)})
 }
 private fun btn(s:String,a:()->Unit){root.addView(Button(this).apply{text=s;textSize=20f;setAllCaps(false);setTextColor(Color.rgb(25,55,100));background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=32f;setStroke(2,Color.rgb(190,218,244))};setPadding(18,16,18,16);setOnClickListener{a()}},LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,9,8,9)})}
 private fun hiddenNextBtn(s:String,a:()->Unit){
  speechNextButton=Button(this).apply{
   text=s;textSize=20f;setAllCaps(false);setTextColor(Color.WHITE);setTypeface(typeface,Typeface.BOLD)
   background=GradientDrawable().apply{setColor(Color.rgb(74,170,103));cornerRadius=32f}
   setPadding(18,16,18,16);visibility=View.GONE;setOnClickListener{a()}
  }
  root.addView(speechNextButton,LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,9,8,9)})
 }
 private fun say(s:String)=tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"cz")
 private fun playWordAndResume(word:String){
  if(!speechScreenActive)return
  cancelListening()
  resumeListeningAfterTts=word
  speechStatus?.text="🔊 Poslouchej slovo…"
  tts.speak(word,TextToSpeech.QUEUE_FLUSH,null,"word_playback")
 }
 private fun retryListeningSoon(delayMs:Long=700L){
  val word=expectedSpeechWord?:return
  if(!speechScreenActive||speechWordPassed)return
  speechHandler.postDelayed({
   if(speechScreenActive&&!speechWordPassed&&expectedSpeechWord==word)startListening(word)
  },delayMs)
 }
 private fun stickerCount(key:String)=prefs.getInt("sticker_"+key,0)
 private fun awardSuccess(world:String?=null):RewardGrant{
  val praise=praisePhrases.random()
  val reward=stickerRewards.random()
  val oldCount=stickerCount(reward.key)
  val isNew=oldCount==0
  prefs.edit().putInt("sticker_"+reward.key,oldCount+1).apply()
  stars+=1
  if(world!=null)addWorldProgress(world)
  val allCollected=stickerRewards.all{stickerCount(it.key)>0}
  val bonus=allCollected&&!prefs.getBoolean("sticker_collection_bonus",false)
  if(bonus){
   prefs.edit().putBoolean("sticker_collection_bonus",true).apply()
   stars+=10
  }
  val spokenReward=if(isNew)"Získáváš novou samolepku "+reward.name+"." else "Získáváš samolepku "+reward.name+"."
  val bonusText=if(bonus)" Máš celou sbírku! Dostáváš bonus deset hvězdiček." else ""
  say(praise+" "+spokenReward+bonusText)
  val toast=(if(isNew)"🆕 " else "🎁 ")+reward.icon+" "+reward.name+"   ⭐ +1"+if(bonus)"   👑 BONUS +10 ⭐" else ""
  Toast.makeText(this,toast,Toast.LENGTH_LONG).show()
  return RewardGrant(praise,reward,isNew,bonus)
 }
 private fun worldKey(world:String)=normalizeSpeech(world).replace(" ","_")
 private fun worldProgress(world:String)=prefs.getInt("world_progress_"+worldKey(world),0)
 private fun worldStars(world:String):Int=(1+worldProgress(world)/4).coerceIn(1,5)
 private fun addWorldProgress(world:String){
  val key="world_progress_"+worldKey(world)
  prefs.edit().putInt(key,worldProgress(world)+1).apply()
 }
 private fun starsRow(world:String):String{
  val n=worldStars(world)
  return "★".repeat(n)+"☆".repeat(5-n)
 }
 private fun currentWorldIndex():Int{
  val i=worldList.indexOfFirst{worldStars(it.first)<5}
  return if(i<0)worldList.lastIndex else i
 }
 private fun worldMapCard(world:String,icon:String,index:Int,active:Boolean){
  if(index>0)text("•   •   •",17)
  if(active)text("🧸  Beri je tady",20,true)
  val outer=LinearLayout(this).apply{
   orientation=LinearLayout.HORIZONTAL
   gravity=if(index%2==0)Gravity.START else Gravity.END
  }
  val card=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL
   gravity=Gravity.CENTER
   setPadding(26,22,26,22)
   background=GradientDrawable().apply{
    setColor(if(active)Color.rgb(255,247,214) else Color.WHITE)
    cornerRadius=38f
    setStroke(if(active)4 else 2,if(active)Color.rgb(244,178,56) else Color.rgb(200,222,239))
   }
   elevation=8f
   isClickable=true
   setOnClickListener{worldHub(world)}
  }
  card.addView(TextView(this).apply{
   text=icon;textSize=46f;gravity=Gravity.CENTER
  })
  card.addView(TextView(this).apply{
   text=world;textSize=22f;gravity=Gravity.CENTER;setTextColor(Color.rgb(28,58,104));setTypeface(typeface,Typeface.BOLD)
  })
  card.addView(TextView(this).apply{
   text=starsRow(world);textSize=24f;gravity=Gravity.CENTER
   setTextColor(Color.rgb(238,169,38))
  })
  card.addView(TextView(this).apply{
   text=if(worldStars(world)==5)"Hotovo" else "Úroveň "+worldStars(world)+" z 5"
   textSize=14f;gravity=Gravity.CENTER;setTextColor(Color.rgb(92,113,143))
  })
  outer.addView(card,LinearLayout.LayoutParams(0,-2,.78f))
  root.addView(outer,LinearLayout.LayoutParams(-1,-2).apply{setMargins(4,6,4,6)})
 }
 private fun worldHub(world:String){
  base()
  val icon=worldIcons[world]?:"⭐"
  text("Berialo",22,true)
  text(icon,74)
  text(world,32,true)
  text(starsRow(world),30,true)
  text("Beri tě provede tímto světem.",18)
  bigBtn("📚 UČENÍ","Obrázek • slovo • poslech",Color.rgb(67,136,230)){learn(world,0)}
  bigBtn("🗣️ VÝSLOVNOST","Poslech • mikrofon • odměna",Color.rgb(151,103,214)){listenWorld(world)}
  bigBtn("🎯 HRA","Najdi správnou odpověď",Color.rgb(255,155,72)){quiz(world)}
  btn("🗺️  Zpět na mapu"){home()}
 }
 private val domecekImages=mapOf(
  "dům" to R.drawable.word_dum,
  "panelák" to R.drawable.word_panelak,
  "pokoj" to R.drawable.word_pokoj,
  "postel" to R.drawable.word_postel,
  "polštář" to R.drawable.word_polstar,
  "deka" to R.drawable.word_deka,
  "stůl" to R.drawable.word_stul,
  "židle" to R.drawable.word_zidle,
  "okno" to R.drawable.word_okno,
  "dveře" to R.drawable.word_dvere
 )
 private fun drawableFor(x:Word):Int?=if(x.world=="Domeček")domecekImages[x.name]else null
 private fun showWordVisual(x:Word){
  val resId=drawableFor(x)
  if(resId!=null){
   root.addView(ImageView(this).apply{
    setImageResource(resId);scaleType=ImageView.ScaleType.FIT_CENTER;contentDescription=x.name;adjustViewBounds=true
   },LinearLayout.LayoutParams(520,520).apply{setMargins(8,12,8,12)})
  }else if(x.world!="Domeček") text(x.icon,112)
 }
 private fun wordBtn(x:Word,a:()->Unit){
  val resId=drawableFor(x)
  if(resId==null){btn(if(x.world=="Domeček") x.name else x.icon+"   "+x.name,a);return}
  val card=LinearLayout(this).apply{
   orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
   background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=32f;setStroke(2,Color.rgb(190,218,244))}
   setPadding(12,12,12,14);setOnClickListener{a()}
  }
  card.addView(ImageView(this).apply{setImageResource(resId);scaleType=ImageView.ScaleType.FIT_CENTER;contentDescription=x.name},
   LinearLayout.LayoutParams(-1,260))
  card.addView(TextView(this).apply{text=x.name;textSize=20f;gravity=Gravity.CENTER;setTextColor(Color.rgb(25,55,100));setTypeface(typeface,Typeface.BOLD)})
  root.addView(card,LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,9,8,9)})
 }
 private fun splash(){
  base();root.gravity=Gravity.CENTER
  val b=TextView(this).apply{text="🧸";textSize=112f;gravity=Gravity.CENTER;alpha=0f;scaleX=.4f;scaleY=.4f}
  root.addView(b)
  text("BERIALO",36,true)
  text("Uč se s Berim",19)
  b.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(650).setInterpolator(OvershootInterpolator())
  Handler(Looper.getMainLooper()).postDelayed({home()},1400)
 }
 private fun home(){
  base()
  root.setPadding(26,28,26,34)
  text("BERIALO",34,true)
  text("🧸 Beriho dobrodružná mapa",20,true)
  text("⭐ "+stars+"     🎁 "+stickerRewards.sumOf{stickerCount(it.key)},18,true)
  text("Vyber svět a pokračuj po cestě.",17)
  val active=currentWorldIndex()
  worldList.forEachIndexed{index,w->worldMapCard(w.first,w.second,index,index==active)}
  btn("🎓  Beriho školní hry"){schoolPrep()}
  btn("🎁  Moje odměny"){rewards()}
 }
 private fun worlds(q:Boolean){
  base();text(if(q)"🎯 Vyber si svět" else "📚 Vyber si svět",30,true)
  worldList.forEach{w->btn(w.second+"   "+w.first+"   "+starsRow(w.first)){if(q)quiz(w.first)else learn(w.first,0)}}
  btn("🗺️ Mapa"){home()}
 }
 private fun learn(world:String,i:Int){
  speechWordPassed=false;speechNextButton=null
  val ws=words.filter{it.world==world};val x=ws[i%ws.size]
  base();speechScreenActive=true;expectedSpeechWord=x.name;expectedSpeechWorld=world
  text(world,25,true);showWordVisual(x);text(x.name.uppercase(),34,true)
  btn("🔊  Přehrát slovo"){playWordAndResume(x.name)}
  speechInfo()
  hiddenNextBtn("Další  ➜"){learn(world,i+1)}
  btn("🎯  Procvičit"){quiz(world)}
  btn("🗺️  Mapa"){home()}
  speechHandler.postDelayed({if(speechScreenActive&&!speechWordPassed)startListening(x.name)},250)
 }
 private fun quiz(world:String){val pool=words.filter{it.world==world};val target=pool.random();val choices=(pool.filter{it!=target}.shuffled().take(3)+target).shuffled();base();text("Najdi správný obrázek",25,true);text("Najdi: "+target.name,30,true);btn("🔊  Přehrát zadání"){say("Najdi "+target.name)};choices.forEach{c->wordBtn(c){if(c==target){awardSuccess(world);quiz(world)}else{say("Zkus to ještě jednou");Toast.makeText(this,"Zkus to ještě jednou",Toast.LENGTH_SHORT).show()}}};btn("⌂  Domů"){home()}}
 private fun listen(){listenWorld(null)}
 private fun listenWorld(world:String?){
  speechWordPassed=false;speechNextButton=null
  val pool=if(world==null)words else words.filter{it.world==world}
  val x=pool.random()
  base();speechScreenActive=true;expectedSpeechWord=x.name;expectedSpeechWorld=x.world
  text("🗣️ Výslovnost",27,true);text(x.world,18,true);showWordVisual(x);text(x.name,34,true)
  btn("🔊  Přehrát slovo"){playWordAndResume(x.name)}
  speechInfo("🎤 Mikrofon poslouchá. Řekni zobrazené slovo.")
  hiddenNextBtn("Další slovo  ➜"){listenWorld(world)}
  btn("🗺️  Mapa"){home()}
  speechHandler.postDelayed({if(speechScreenActive&&!speechWordPassed)startListening(x.name)},250)
 }
 private fun startListening(word:String){
  expectedSpeechWord=word
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
   requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),REQ_RECORD_AUDIO)
   return
  }
  beginSpeechRecognition()
 }
 private fun beginSpeechRecognition(){
  val target=expectedSpeechWord?:return
  if(!speechScreenActive||speechWordPassed)return
  if(!SpeechRecognizer.isRecognitionAvailable(this)){
   speechStatus?.text="Rozpoznávání řeči není v tomto telefonu dostupné."
   Toast.makeText(this,"Rozpoznávání řeči není dostupné",Toast.LENGTH_LONG).show()
   return
  }
  if(speechRecognizer==null){
   speechRecognizer=SpeechRecognizer.createSpeechRecognizer(this).also{r->
    r.setRecognitionListener(object:RecognitionListener{
     override fun onReadyForSpeech(params:Bundle?){speechStatus?.text="🎤 Poslouchám… řekni slovo."}
     override fun onBeginningOfSpeech(){speechStatus?.text="🎤 Slyším tě…"}
     override fun onRmsChanged(rmsdB:Float){}
     override fun onBufferReceived(buffer:ByteArray?){}
     override fun onEndOfSpeech(){speechStatus?.text="Kontroluji slovo…"}
     override fun onError(error:Int){
      speechStatus?.text=when(error){
       SpeechRecognizer.ERROR_NO_MATCH->"Nerozuměl jsem. Poslouchám znovu…"
       SpeechRecognizer.ERROR_SPEECH_TIMEOUT->"Nic jsem neslyšel. Poslouchám znovu…"
       SpeechRecognizer.ERROR_AUDIO->"Mikrofon měl problém. Zkouším znovu…"
       SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS->"Aplikace nemá povolený mikrofon."
       SpeechRecognizer.ERROR_NETWORK,SpeechRecognizer.ERROR_NETWORK_TIMEOUT->"Rozpoznávání řeči teď není dostupné. Zkouším znovu…"
       else->"Nepodařilo se rozpoznat slovo. Poslouchám znovu…"
      }
      if(error!=SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)retryListeningSoon(900)
     }
     override fun onResults(results:Bundle?){
      val heard=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
      checkSpokenWord(heard)
     }
     override fun onPartialResults(partialResults:Bundle?){}
     override fun onEvent(eventType:Int,params:Bundle?){}
    })
   }
  }
  speechStatus?.text="🎤 Připravuji mikrofon…"
  val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{
   putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
   putExtra(RecognizerIntent.EXTRA_LANGUAGE,"cs-CZ")
   putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5)
   putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false)
   putExtra(RecognizerIntent.EXTRA_PROMPT,target)
  }
  try{
   speechRecognizer?.cancel()
   speechRecognizer?.startListening(intent)
  }catch(_:Exception){
   speechStatus?.text="Mikrofon se nepodařilo spustit. Zkus to znovu."
  }
 }
 private fun normalizeSpeech(s:String):String{
  val plain=Normalizer.normalize(s.lowercase(Locale("cs","CZ")),Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"),"")
  return plain.replace(Regex("[^a-z0-9 ]")," ").replace(Regex("\\s+")," ").trim()
 }
 private fun checkSpokenWord(results:List<String>){
  val target=expectedSpeechWord?:return
  val wanted=normalizeSpeech(target)
  val pattern=Regex("(^| )"+Regex.escape(wanted)+"($| )")
  val ok=results.any{candidate->pattern.containsMatchIn(normalizeSpeech(candidate))}
  if(ok){
   if(!speechWordPassed){
    speechWordPassed=true
    val grant=awardSuccess(expectedSpeechWorld)
    speechStatus?.text="✅ "+grant.praise+"  "+grant.reward.icon+" "+grant.reward.name+"  •  Teď můžeš pokračovat dál."
   }else{
    speechStatus?.text="✅ Tohle slovo už máš splněné. Můžeš pokračovat dál."
   }
   speechNextButton?.visibility=View.VISIBLE
  }else{
   val best=results.firstOrNull()?.trim().orEmpty()
   speechStatus?.text=if(best.isBlank())"Nerozuměl jsem. Poslouchám znovu…" else "Slyšel jsem „"+best+"“. Poslouchám znovu, řekni: "+target+"."
   retryListeningSoon(700)
  }
 }
 private fun cancelListening(){try{speechRecognizer?.cancel()}catch(_:Exception){}}
 override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){
  super.onRequestPermissionsResult(requestCode,permissions,grantResults)
  if(requestCode==REQ_RECORD_AUDIO){
   if(grantResults.isNotEmpty()&&grantResults[0]==PackageManager.PERMISSION_GRANTED)beginSpeechRecognition()
   else speechStatus?.text="Bez povolení mikrofonu nemůžu poslouchat výslovnost."
  }
 }
 private fun schoolPrep(){base();text("🎓 Příprava do školy",30,true);text("Vyber si hru",22,true);bigBtn("📍 KDE TO JE?","na • pod • vedle • před • za",Color.rgb(91,155,213)){positionGame()};bigBtn("🔢 POČÍTÁNÍ","od 1 do 20",Color.rgb(245,166,35)){countGame()};bigBtn("↔️ PROTIKLADY","velký × malý",Color.rgb(139,101,207)){oppositesGame()};bigBtn("🧠 ROZUMÍM","pokyny a situace",Color.rgb(70,170,120)){instructionGame()};btn("🏠 Domů"){home()}}
 private fun positionGame(){val tasks=listOf("Míč je NA stole." to "na","Kočka je POD stolem." to "pod","Medvídek je VEDLE židle." to "vedle","Auto je PŘED domem." to "před","Pes je ZA domem." to "za","Kostka je V krabici." to "v","Židle je MEZI stoly." to "mezi");val t=tasks.random();base();text("Kde to je?",28,true);text(t.first,27,true);btn("🔊 Poslechni"){say(t.first)};listOf("na","pod","vedle","před","za","v","mezi").shuffled().take(4).let{xs->val opts=(xs+t.second).distinct().shuffled().take(4);opts.forEach{o->btn(o.uppercase()){if(o==t.second){awardSuccess("Škola");positionGame()}else say("Zkus to znovu")}}};btn("⌂ Domů"){home()}}
 private fun countGame(){val n=(1..20).random();val options=listOf(n,(n+1).coerceAtMost(20),(n-1).coerceAtLeast(1),(1..20).random()).distinct().shuffled();base();text("Počítání do 20",28,true);text("⭐ ".repeat(n),22);text("Kolik je hvězdiček?",22,true);options.forEach{o->btn(o.toString()){if(o==n){awardSuccess("Škola");countGame()}else say("Zkus to znovu")}};btn("⌂ Domů"){home()}}
 private fun oppositesGame(){val pairs=listOf("velký" to "malý","rychlý" to "pomalý","teplý" to "studený","nahoře" to "dole","den" to "noc","plný" to "prázdný","otevřený" to "zavřený","veselý" to "smutný","dlouhý" to "krátký","čistý" to "špinavý");val p=pairs.random();val choices=(pairs.flatMap{listOf(it.first,it.second)}.filter{it!=p.first}.shuffled().take(3)+p.second).shuffled();base();text("Najdi protiklad",28,true);text(p.first.uppercase(),32,true);choices.forEach{o->btn(o){if(o==p.second){awardSuccess("Škola");oppositesGame()}else say("Zkus to znovu")}};btn("⌂ Domů"){home()}}
 private fun instructionGame(){val tasks=listOf("Co uděláš, když učitel řekne: Otevři knihu?" to "otevřu knihu","Co uděláš před přechodem přes silnici?" to "rozhlédnu se","Co řekneš, když o něco žádáš?" to "prosím","Co řekneš, když ti někdo pomůže?" to "děkuji","Kterou rukou ukazuješ doprava?" to "pravou");val t=tasks.random();val wrong=listOf("zavřu oči","uteču","nevím","nic neřeknu","otočím se");val choices=(wrong.shuffled().take(3)+t.second).shuffled();base();text("Rozumím pokynům",28,true);text(t.first,22,true);btn("🔊 Poslechni"){say(t.first)};choices.forEach{o->btn(o){if(o==t.second){awardSuccess("Škola");instructionGame()}else say("Zkus to znovu")}};btn("⌂ Domů"){home()}}
 private fun rewards(){
  base()
  text("🎁 Moje odměny",30,true)
  text("⭐ "+stars,42,true)
  val collected=stickerRewards.count{stickerCount(it.key)>0}
  text("Samolepky "+collected+" / "+stickerRewards.size,22,true)
  stickerRewards.forEach{r->
   val count=stickerCount(r.key)
   text(if(count>0)r.icon+"  "+r.name+"  × "+count else "🔒  "+r.name,20)
  }
  if(collected==stickerRewards.size)text("👑 Celá sbírka hotová! Bonus +10 ⭐",20,true)
  else text("Nasbírej všech 10 různých samolepek a dostaneš bonus +10 ⭐.",17)
  btn("🎯  Získat další odměnu"){worlds(true)}
  btn("⌂  Domů"){home()}
 }
 override fun onBackPressed(){home()}
 override fun onDestroy(){try{speechRecognizer?.cancel();speechRecognizer?.destroy();speechRecognizer=null}catch(_:Exception){};tts.stop();tts.shutdown();super.onDestroy()}
}