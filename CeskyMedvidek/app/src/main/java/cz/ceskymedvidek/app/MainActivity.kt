package cz.ceskymedvidek.app
import android.app.*
import android.os.*
import android.speech.tts.TextToSpeech
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.BitmapDrawable
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.*
import java.util.Locale

data class Word(val name:String,val icon:String,val world:String)

class MainActivity:Activity(),TextToSpeech.OnInitListener{
 private lateinit var root:LinearLayout
 private lateinit var tts:TextToSpeech
 private val prefs by lazy{getSharedPreferences("medvidek",MODE_PRIVATE)}
 private var stars:Int get()=prefs.getInt("stars",0); set(v){prefs.edit().putInt("stars",v).apply()}
 private val vocabulary=mapOf(
 "Domeček" to listOf("dům","byt","pokoj","postel","polštář","deka","stůl","židle","okno","dveře","lampa","skříň","pohovka","televize","telefon","kniha","hračka","míč","kočka","pes","jíst","pít","spát","sedět","stát"),
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
 override fun onInit(s:Int){if(s==TextToSpeech.SUCCESS){tts.language=Locale("cs","CZ");tts.setSpeechRate(.82f)}}
 private fun base(){root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(28,42,28,28);background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(Color.rgb(220,246,255),Color.rgb(247,238,255)))};setContentView(ScrollView(this).apply{addView(root)})}
 private fun text(s:String,size:Int=22,bold:Boolean=false){root.addView(TextView(this).apply{text=s;textSize=size.toFloat();gravity=Gravity.CENTER;setTextColor(Color.rgb(31,61,112));if(bold)setTypeface(typeface,Typeface.BOLD);setPadding(8,10,8,10)})}
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
 private fun say(s:String)=tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"cz")
 private val domecekImageWords=listOf("dům","byt","pokoj","postel","polštář","deka","stůl","židle","okno","dveře","lampa","skříň","pohovka","televize","telefon","kniha","hračka","míč","kočka","pes","jíst","pít","spát","sedět","stát")
 private val homeSheet:Bitmap by lazy{BitmapFactory.decodeResource(resources,R.drawable.home_sprite)}
 private fun homeBitmapFor(word:String):Bitmap?{
  val index=domecekImageWords.indexOf(word)
  if(index<0)return null
  return try{
   val cols=5
   val rows=5
   val cellW=homeSheet.width/cols
   val cellH=homeSheet.height/rows
   val x=(index%cols)*cellW
   val y=(index/cols)*cellH
   if(cellW<=0||cellH<=0||x+cellW>homeSheet.width||y+cellH>homeSheet.height)null
   else Bitmap.createBitmap(homeSheet,x,y,cellW,cellH)
  }catch(e:Exception){null}
 }
 private fun showWordVisual(x:Word){
  val bmp=if(x.world=="Domeček")homeBitmapFor(x.name)else null
  if(bmp!=null){
   root.addView(ImageView(this).apply{setImageBitmap(bmp);scaleType=ImageView.ScaleType.FIT_CENTER;contentDescription=x.name},LinearLayout.LayoutParams(520,420).apply{setMargins(8,12,8,12)})
  }else text(x.icon,112)
 }
 private fun wordBtn(x:Word,a:()->Unit){
  val bmp=if(x.world=="Domeček")homeBitmapFor(x.name)else null
  if(bmp==null){btn(x.icon+"   "+x.name,a);return}
  root.addView(Button(this).apply{
   text=x.name;textSize=20f;setAllCaps(false);setTextColor(Color.rgb(25,55,100))
   background=GradientDrawable().apply{setColor(Color.WHITE);cornerRadius=32f;setStroke(2,Color.rgb(190,218,244))}
   val d=BitmapDrawable(resources,bmp);d.setBounds(0,0,210,160);setCompoundDrawables(null,d,null,null);compoundDrawablePadding=8
   setPadding(12,14,12,14);setOnClickListener{a()}
  },LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,9,8,9)})
 }
 private fun splash(){base();root.gravity=Gravity.CENTER;val b=TextView(this).apply{text="🧸";textSize=112f;gravity=Gravity.CENTER;alpha=0f;scaleX=.4f;scaleY=.4f};root.addView(b);text("Český medvídek",34,true);b.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).setInterpolator(OvershootInterpolator());Handler(Looper.getMainLooper()).postDelayed({home()},2200)}
 private fun home(){base();root.setPadding(32,34,32,34);text("Český medvídek",34,true);text("🧸",88);text("Co si dnes dáme?",24,true);text("⭐ "+stars+"     🏆 "+(1+stars/20),18,true);bigBtn("📚 UČÍM SE","Obrázky • slova • poslech",Color.rgb(67,136,230)){worlds(false)};bigBtn("🎯 HRAJU SI","Najdi správný obrázek",Color.rgb(255,155,72)){worlds(true)};bigBtn("🎓 DO ŠKOLY","Počítání • pokyny • orientace",Color.rgb(89,180,120)){schoolPrep()};btn("🎁 Moje odměny"){rewards()}}
 private fun worlds(q:Boolean){base();text(if(q)"🎯 Vyber si svět" else "📚 Vyber si svět",30,true);text(if(q)"Kde si chceš zahrát?" else "Co se chceš učit?",20);worldList.forEach{w->btn(w.second+"   "+w.first){if(q)quiz(w.first)else learn(w.first,0)}};btn("🏠 Domů"){home()}}
 private fun learn(world:String,i:Int){val ws=words.filter{it.world==world};val x=ws[i%ws.size];base();text(world,25,true);showWordVisual(x);text(x.name.uppercase(),34,true);btn("🔊  Poslechni si"){say(x.name)};btn("Další  ➜"){learn(world,i+1)};btn("🎯  Procvičit"){quiz(world)};btn("⌂  Domů"){home()}}
 private fun quiz(world:String){val pool=words.filter{it.world==world};val target=pool.random();val choices=(pool.filter{it!=target}.shuffled().take(3)+target).shuffled();base();text("Najdi správný obrázek",25,true);text("Najdi: "+target.name,30,true);btn("🔊  Přehrát zadání"){say("Najdi "+target.name)};choices.forEach{c->wordBtn(c){if(c==target){stars+=1;say("Výborně");Toast.makeText(this,"⭐ +1 hvězdička",Toast.LENGTH_SHORT).show();quiz(world)}else{say("Zkus to ještě jednou");Toast.makeText(this,"Zkus to ještě jednou",Toast.LENGTH_SHORT).show()}}};btn("⌂  Domů"){home()}}
 private fun listen(){val x=words.random();base();text("Poslouchej a opakuj",27,true);showWordVisual(x);text(x.name,34,true);btn("🔊  Přehrát slovo"){say(x.name)};btn("Další slovo"){listen()};btn("⌂  Domů"){home()}}
 private fun schoolPrep(){base();text("🎓 Příprava do školy",30,true);text("Vyber si hru",22,true);bigBtn("📍 KDE TO JE?","na • pod • vedle • před • za",Color.rgb(91,155,213)){positionGame()};bigBtn("🔢 POČÍTÁNÍ","od 1 do 20",Color.rgb(245,166,35)){countGame()};bigBtn("↔️ PROTIKLADY","velký × malý",Color.rgb(139,101,207)){oppositesGame()};bigBtn("🧠 ROZUMÍM","pokyny a situace",Color.rgb(70,170,120)){instructionGame()};btn("🏠 Domů"){home()}}
 private fun positionGame(){val tasks=listOf("Míč je NA stole." to "na","Kočka je POD stolem." to "pod","Medvídek je VEDLE židle." to "vedle","Auto je PŘED domem." to "před","Pes je ZA domem." to "za","Kostka je V krabici." to "v","Židle je MEZI stoly." to "mezi");val t=tasks.random();base();text("Kde to je?",28,true);text(t.first,27,true);btn("🔊 Poslechni"){say(t.first)};listOf("na","pod","vedle","před","za","v","mezi").shuffled().take(4).let{xs->val opts=(xs+t.second).distinct().shuffled().take(4);opts.forEach{o->btn(o.uppercase()){if(o==t.second){stars++;say("Výborně");positionGame()}else say("Zkus to znovu")}}};btn("⌂ Domů"){home()}}
 private fun countGame(){val n=(1..20).random();val options=listOf(n,(n+1).coerceAtMost(20),(n-1).coerceAtLeast(1),(1..20).random()).distinct().shuffled();base();text("Počítání do 20",28,true);text("⭐ ".repeat(n),22);text("Kolik je hvězdiček?",22,true);options.forEach{o->btn(o.toString()){if(o==n){stars++;say("Výborně");countGame()}else say("Zkus to znovu")}};btn("⌂ Domů"){home()}}
 private fun oppositesGame(){val pairs=listOf("velký" to "malý","rychlý" to "pomalý","teplý" to "studený","nahoře" to "dole","den" to "noc","plný" to "prázdný","otevřený" to "zavřený","veselý" to "smutný","dlouhý" to "krátký","čistý" to "špinavý");val p=pairs.random();val choices=(pairs.flatMap{listOf(it.first,it.second)}.filter{it!=p.first}.shuffled().take(3)+p.second).shuffled();base();text("Najdi protiklad",28,true);text(p.first.uppercase(),32,true);choices.forEach{o->btn(o){if(o==p.second){stars++;say("Výborně");oppositesGame()}else say("Zkus to znovu")}};btn("⌂ Domů"){home()}}
 private fun instructionGame(){val tasks=listOf("Co uděláš, když učitel řekne: Otevři knihu?" to "otevřu knihu","Co uděláš před přechodem přes silnici?" to "rozhlédnu se","Co řekneš, když o něco žádáš?" to "prosím","Co řekneš, když ti někdo pomůže?" to "děkuji","Kterou rukou ukazuješ doprava?" to "pravou");val t=tasks.random();val wrong=listOf("zavřu oči","uteču","nevím","nic neřeknu","otočím se");val choices=(wrong.shuffled().take(3)+t.second).shuffled();base();text("Rozumím pokynům",28,true);text(t.first,22,true);btn("🔊 Poslechni"){say(t.first)};choices.forEach{o->btn(o){if(o==t.second){stars++;say("Výborně");instructionGame()}else say("Zkus to znovu")}};btn("⌂ Domů"){home()}}
 private fun rewards(){base();text("Moje odměny",30,true);text("⭐ "+stars,42,true);val r=listOf(5 to "🎈 Balónek",10 to "🧢 Čepice",20 to "🧸 Plyšák",35 to "👓 Brýle",50 to "🏆 Zlatý pohár",100 to "👑 Koruna");r.forEach{(n,name)->text(if(stars>=n)"✅ "+name else "🔒 "+name+"  •  "+n+" ⭐",20)};btn("🎯  Získat hvězdičky"){worlds(true)};btn("⌂  Domů"){home()}}
 override fun onBackPressed(){home()}
 override fun onDestroy(){tts.stop();tts.shutdown();super.onDestroy()}
}