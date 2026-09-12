package com.noxforgestudios.mygarage.domain

import java.text.Normalizer
import java.util.Locale

/** Offline starting catalogue. Custom entries are always accepted. */
object VehicleCatalog {
    val fuels = listOf(
        "Gasolina 95 RON", "Gasolina 98 RON", "Gasolina 100 RON", "Gasolina 102 RON",
        "Gasolina E5", "Gasolina E10", "Gasolina de competición 104 RON", "Gasolina de competición 108 RON",
        "Gasolina de competición 110 RON", "Gasolina de competición 116 RON",
        "Diésel", "Diésel premium", "Biodiésel B7", "Biodiésel B10", "Biodiésel B20", "Biodiésel B100", "HVO / XTL",
        "Etanol E20", "Etanol E30", "Etanol E50", "Etanol E85", "Etanol E100", "Flex-fuel gasolina / etanol",
        "Metanol M100", "Mezcla metanol / nitrometano", "GLP / Autogás", "GNC", "GNL",
        "Hidrógeno", "Eléctrico", "Híbrido gasolina", "Híbrido diésel", "Híbrido enchufable gasolina",
        "Híbrido enchufable diésel", "Gasolina sintética / e-fuel", "Mezcla 2T"
    )
    val transmissions = listOf("Manual", "Automática", "Automática de doble embrague (DCT / DSG)", "CVT", "Secuencial", "Manual robotizada", "Reductora eléctrica")
    val drivetrains = listOf("Trasera (RWD)", "Delantera (FWD)", "Total (AWD)", "4x4 conectable (4WD)")
    val upgradeCategories = listOf("Refrigeración / Intercooler", "Motor", "Turbo / Compresor", "Admisión", "Escape", "Electrónica / ECU", "Combustible", "Suspensión", "Frenos", "Embrague / Caja", "Diferencial", "Ruedas", "Neumáticos", "Interior", "Exterior / Aero", "Seguridad / Jaula", "Iluminación", "Audio", "Otros")
    val upgradeParts = listOf("Intercooler", "Turbo", "Compresor", "Radiador", "Radiador de aceite", "Admisión", "Filtro de aire", "Downpipe", "Escape", "Colectores", "Inyectores", "Bomba de combustible", "ECU / Reprogramación", "Embrague", "Volante motor", "Diferencial autoblocante", "Coilovers", "Amortiguadores", "Muelles", "Barras estabilizadoras", "Pinzas de freno", "Discos", "Pastillas", "Latiguillos", "Llantas", "Neumáticos", "Semislicks", "Baquet", "Arnés", "Jaula", "Alerón", "Splitter", "Difusor", "Manómetro", "Otra pieza")
    val models: Map<String, List<String>> = """
        Abarth|500;595;695;124 Spider;600e
        Acura|Integra;NSX;TLX;RDX;MDX
        Alfa Romeo|33;75;145;146;147;155;156;159;164;166;4C;8C;Brera;Giulia;Giulietta;GT;GTV;MiTo;Spider;Stelvio;Tonale;Junior
        Alpine|A110;A310;A610;A290;GTA
        Aston Martin|DB7;DB9;DB11;DB12;DBS;DBX;Vantage;Vanquish;Rapide
        Audi|80;90;100;200;A1;A2;A3;A4;A5;A6;A7;A8;S1;S3;S4;S5;S6;S7;S8;RS2;RS3;RS4;RS5;RS6;RS7;TT;TTS;TT RS;R8;Q2;Q3;Q4 e-tron;Q5;Q6 e-tron;Q7;Q8;RS Q3;RS Q8;e-tron GT
        Bentley|Continental GT;Flying Spur;Bentayga;Mulsanne;Arnage
        BMW|Serie 1;Serie 2;Serie 3;Serie 4;Serie 5;Serie 6;Serie 7;Serie 8;1M;M2;M3;M4;M5;M6;M8;X1;X2;X3;X4;X5;X6;X7;XM;X3 M;X4 M;X5 M;X6 M;Z1;Z3;Z4;Z8;i3;i4;i5;i7;i8;iX;iX1;iX2;2002
        Bugatti|EB110;Veyron;Chiron;Divo;Tourbillon
        BYD|Atto 3;Dolphin;Seal;Seal U;Han;Tang;Sealion 7
        Cadillac|CTS;ATS;CT4;CT5;Escalade;Lyriq
        Caterham|Seven;Super Seven
        Chevrolet|Camaro;Corvette;Cruze;Aveo;Spark;Malibu;Impala;Silverado;Tahoe;Suburban;Blazer
        Chrysler|300;300C;Crossfire;PT Cruiser;Voyager
        Citroën|AX;BX;CX;ZX;Saxo;Xantia;Xsara;Xsara Picasso;C1;C2;C3;C3 Aircross;C4;C4 Cactus;C4 Picasso;C5;C5 Aircross;C5 X;C6;Berlingo;2CV;DS;SM
        Cupra|Formentor;León;Ateca;Born;Tavascan;Terramar
        Dacia|Sandero;Logan;Duster;Jogger;Spring;Bigster
        Daewoo|Lanos;Nubira;Leganza;Matiz;Lacetti
        Daihatsu|Charade;Copen;Cuore;Terios;Sirion
        Dodge|Challenger;Charger;Viper;Dart;Durango;Ram
        DS|DS 3;DS 4;DS 5;DS 7;DS 9
        Ferrari|F355;360;F430;458;488;F8;296;SF90;812;12Cilindri;Roma;Portofino;California;F40;F50;Enzo;LaFerrari;Purosangue
        Fiat|500;500X;500L;600;Panda;Punto;Grande Punto;Bravo;Brava;Tipo;Stilo;Coupé;Barchetta;124 Spider;Uno;Seicento;Cinquecento;Multipla
        Ford|Fiesta;Focus;Escort;Sierra;Mondeo;Mustang;Mustang Mach-E;Puma;Kuga;Capri;Ka;GT;Probe;Cougar;Ranger;F-150;Bronco;Explorer;Transit;Tourneo
        Genesis|G70;G80;G90;GV60;GV70;GV80
        Honda|Civic;Accord;Integra;Prelude;CRX;CR-Z;S2000;NSX;Jazz;Fit;HR-V;CR-V;ZR-V;e;e:Ny1
        Hyundai|i10;i20;i20 N;i30;i30 N;i40;Veloster;Genesis Coupé;Tiburon;Coupé;Getz;Kona;Tucson;Santa Fe;Ioniq;Ioniq 5;Ioniq 5 N;Ioniq 6
        Infiniti|G35;G37;Q30;Q50;Q60;Q70;QX30;QX50;QX60;FX
        Isuzu|D-Max;Trooper;Piazza
        Jaguar|XE;XF;XJ;XK;X-Type;S-Type;F-Type;E-Pace;F-Pace;I-Pace;E-Type
        Jeep|Wrangler;Cherokee;Grand Cherokee;Compass;Renegade;Avenger;Gladiator
        Kia|Picanto;Rio;Ceed;ProCeed;XCeed;Stinger;Sportage;Sorento;Niro;EV3;EV4;EV5;EV6;EV9;Optima
        Koenigsegg|CCX;Agera;Regera;Jesko;Gemera
        KTM|X-Bow
        Lada|Niva;Samara;2101;2105;2107
        Lamborghini|Miura;Countach;Diablo;Murciélago;Gallardo;Huracán;Aventador;Revuelto;Temerario;Urus
        Lancia|Delta;Integrale;Ypsilon;Thema;Dedra;Lybra;Kappa;Stratos;Fulvia;Beta
        Land Rover|Defender;Discovery;Discovery Sport;Freelander;Range Rover;Range Rover Sport;Range Rover Evoque;Range Rover Velar
        Lexus|IS;ES;GS;LS;RC;RC F;LC;LFA;CT;UX;NX;RX;RZ;LBX
        Lotus|Elise;Exige;Evora;Emira;Esprit;Eletre;Emeya
        Maserati|Ghibli;Quattroporte;GranTurismo;GranCabrio;MC20;Levante;Grecale;3200 GT
        Mazda|MX-5;RX-7;RX-8;2;3;6;323;626;MX-3;MX-6;CX-3;CX-30;CX-5;CX-60;CX-80;MX-30
        McLaren|540C;570S;600LT;620R;650S;675LT;720S;750S;765LT;Artura;GT;GTS;P1;Senna;F1
        Mercedes-Benz|Clase A;Clase B;Clase C;Clase E;Clase S;Clase G;CLA;CLS;CLK;CLC;CL;SL;SLK;SLC;SLS AMG;AMG GT;GLA;GLB;GLC;GLE;GLS;GLK;ML;EQA;EQB;EQC;EQE;EQS;190;Vito;Clase V
        MG|MG3;MG4;MG5;ZS;HS;Cyberster;TF;F;ZR;ZT
        MINI|Cooper;Cooper S;John Cooper Works;Clubman;Countryman;Paceman;Coupé;Roadster;Aceman
        Mitsubishi|Lancer;Lancer Evolution;Colt;Eclipse;3000GT;Galant;ASX;Outlander;Montero;Pajero;L200;Space Star;FTO;Starion
        Nissan|Micra;Almera;Primera;Sunny;Pulsar;Sentra;200SX;240SX;Silvia;300ZX;350Z;370Z;Z;Skyline;GT-R;Juke;Qashqai;X-Trail;Patrol;Navara;Leaf;Ariya
        Opel|Corsa;Astra;Kadett;Vectra;Omega;Calibra;Tigra;Speedster;Insignia;Manta;Monza;Adam;Meriva;Zafira;Mokka;Crossland;Grandland;Frontera
        Peugeot|106;107;108;205;206;207;208;208 GTi;306;307;308;309;405;406;407;408;508;605;607;RCZ;1007;2008;3008;5008;Partner;Rifter
        Polestar|1;2;3;4
        Pontiac|Firebird;Trans Am;GTO;Solstice;Fiero;Grand Prix
        Porsche|911;718;Boxster;Cayman;944;968;928;924;Carrera GT;918 Spyder;Panamera;Cayenne;Macan;Taycan
        Renault|Clio;Mégane;Scénic;Laguna;Safrane;Talisman;Captur;Kadjar;Koleos;Austral;Rafale;Espace;Twingo;5;19;21;25;Wind;Fluence;Zoe;Arkana;Kangoo;4
        Rolls-Royce|Phantom;Ghost;Wraith;Dawn;Cullinan;Spectre
        Rover|25;45;75;200;400;600;800;Mini
        Saab|900;9000;9-3;9-5;9-4X
        SEAT|Ibiza;León;Córdoba;Toledo;Altea;Arosa;Mii;Arona;Ateca;Tarraco;Alhambra;Marbella;124;127;131;600
        Škoda|Fabia;Octavia;Superb;Scala;Kamiq;Karoq;Kodiaq;Enyaq;Elroq;Rapid;Felicia;Citigo
        Smart|Fortwo;Forfour;Roadster;#1;#3;#5
        SsangYong|Tivoli;Korando;Rexton;Musso;Torres
        Subaru|Impreza;WRX;WRX STI;BRZ;Legacy;Outback;Forester;XV;Crosstrek;Levorg;SVX;Justy;Solterra
        Suzuki|Swift;Swift Sport;Ignis;Vitara;Grand Vitara;Jimny;Samurai;SX4;S-Cross;Baleno;Alto;Kizashi;Across;Swace
        Tesla|Model S;Model 3;Model X;Model Y;Roadster;Cybertruck
        Toyota|Yaris;GR Yaris;Corolla;GR Corolla;Supra;GR Supra;GT86;GR86;Celica;MR2;Starlet;AE86;Aygo;Auris;Avensis;Camry;Prius;C-HR;RAV4;Land Cruiser;Hilux;bZ4X
        Volkswagen|Golf;Polo;Passat;Scirocco;Corrado;Lupo;Up!;Jetta;Bora;Vento;Beetle;Arteon;Touran;Sharan;T-Roc;T-Cross;Tiguan;Touareg;Taigo;ID.3;ID.4;ID.5;ID.7;ID. Buzz;Transporter;Caddy;Amarok
        Volvo|240;740;850;940;960;C30;C70;S40;S60;S70;S80;S90;V40;V50;V60;V70;V90;XC40;XC60;XC70;XC90;EX30;EX40;EX90
    """.trimIndent().lines().associate { line -> line.substringBefore('|') to line.substringAfter('|').split(';') }

    fun searchKey(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).trim()
    fun modelsFor(make: String): List<String> = models.entries.firstOrNull { searchKey(it.key) == searchKey(make) }?.value.orEmpty()
    fun generationsFor(make: String, model: String): List<String> = when {
        searchKey(make) == "bmw" && model == "Serie 3" -> listOf("E21", "E30", "E36", "E46", "E90 / E91 / E92 / E93", "F30 / F31 / F34", "G20 / G21")
        searchKey(make) == "bmw" && model == "M3" -> listOf("E30", "E36", "E46", "E90 / E92 / E93", "F80", "G80 / G81")
        searchKey(make) == "bmw" && model == "Serie 5" -> listOf("E12", "E28", "E34", "E39", "E60 / E61", "F10 / F11", "G30 / G31", "G60 / G61")
        searchKey(make) == "bmw" && model == "M5" -> listOf("E28", "E34", "E39", "E60 / E61", "F10", "F90", "G90 / G99")
        searchKey(make) == "volkswagen" && model == "Golf" -> (1..8).map { "Golf $it" }
        searchKey(make) == "mazda" && model == "MX-5" -> listOf("NA", "NB", "NC", "ND")
        searchKey(make) == "seat" && model == "León" -> listOf("1M", "1P", "5F", "KL")
        else -> emptyList()
    }
}
