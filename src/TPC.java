import com.comsol.model.*;
import com.comsol.model.util.*;

public class TPC {
	public Model model;

	public double TPCRadius = 800; // mm? Need to verify!
	public double electrodeThickness = 1;
	public double TPCLength(){
		return (FSELength + FSEzSpacing) * (FSENumber)+2*(FSEzSpacing+FSELength/2)-FSEzSpacing;
	}
	public double FSEOuterRadius(){
		return TPCRadius + 2*FSEThickness + FSErSpacing;
	}
	                             // FSE is strips.
	public int FSENumber = 80; // Number of strips, 80 
	public double FSELength = 9.0; // Strip length, 9.0 (mm)
	public double FSEzSpacing = 1.0; // Strip spacing in z, 1 (mm)
	public double FSEThickness = .035; // Strip thickness, 
	public double FSErSpacing = .05; // Kapton tape length
	public double offsetz() { return FSELength + FSEzSpacing;}
	public double punchthroughThickness = .035; //Don't know what this is.
	public double mirrorLength = 4.6; // Don't know what this is.
	public double cageThickness = 10; // first cage parameter
	public double cageEndSpacing = 300; // second cage parameter
	public double cageSideSpacing = 150; // third cage parameter
	
	public double Resistance = 1000000;
	public double Conductivity = .000004;
	public double Voltage = 23000; //Voltage between one end and the middle membrane 34,000 Volts
	
	public static void main(String[] args){
		run();
	}
	public static Model run(){
		return new TPC().model;
	}
	public TPC(){
		this.model = ModelUtil.create("Model");
		this.model.modelNode().create("comp1");
		
		this.setVariables();
		this.makeGeometry();
		this.makeSelections();
		this.makeTerminals();
		this.makeCircuit();
		this.setMaterials();
		this.makeDataSet();
		this.makeSolver();
	    this.model.mesh().create("mesh1", "geom");

	}
	
	public void setVariables(){}
	
	public void makeGeometry(){
		this.model.geom().create("geom", 2);
		this.model.geom("geom").axisymmetric(true);
		this.model.geom("geom").lengthUnit("mm");
		
		this.addRect("anodeRect",0,-electrodeThickness,TPCRadius,1);
		this.addRect("cathodeRect",0,TPCLength(),TPCRadius,1);
		this.addFSEs();
		
		//double cagez =-electrodeThickness-cageEndSpacing;
		//double cagew = FSEOuterRadius()+cageSideSpacing;
		//double cageh = TPCLength()+2*electrodeThickness+2*cageEndSpacing;
		//this.addRect("cageInRect",0,cagez,cagew,cageh);
		//this.addRect("cageOutRect",0,cagez-cageThickness,cagew+cageThickness,cageh+cageThickness*2);
		this.addCircle("airsphere",2000,TPCLength()/2); // changed airsphere radius from 1000 to 2000
		this.model.geom("geom").run();
	}
	public void addRect(String name, double r, double z, double t, double h){
		double size[] = {t, h};
		double pos[] = {r, z};

		this.model.geom("geom").feature().create(name,"Rectangle");
		this.model.geom("geom").feature(name).set("pos",pos);
		this.model.geom("geom").feature(name).set("size",size);
	}
	public void addFSEs(){}//						THIS METHOD DOSES NOT FUNCITON AND MUST BE OVERRIDDEN
	public void makeFSEArray(double offset,String[] inputs,int size){
		this.model.geom("geom").feature().create("FSEArray","Array");
		this.model.geom("geom").feature("FSEArray").selection("input").set(inputs);
		this.model.geom("geom").feature("FSEArray").setIndex("displ","0",0);
		this.model.geom("geom").feature("FSEArray").setIndex("displ",offset,1);
		this.model.geom("geom").feature("FSEArray").setIndex("fullsize","1",0);
		this.model.geom("geom").feature("FSEArray").setIndex("fullsize",size+"",1);
	}
	public void addCircle(String name, double radius, double center){
		this.model.geom("geom").feature().create(name,"Circle");
		this.model.geom("geom").feature(name).set("r",radius);
		this.model.geom("geom").feature(name).set("pos", new double[] {0,center});
	}

	public void makeSelections(){
		this.makeAnodeSelection();
		this.makeCathodeSelection();
		for(int i = 0; i < FSENumber; i++){
			this.makeFSESelection(i);
		}
		//this.makeCageSelection();
	}
	public void makeAnodeSelection(){ 
		this.model.selection().create("anodeSelection","Box");
		this.model.selection("anodeSelection").set("entitydim",1);
		this.model.selection("anodeSelection").set("xmin",TPCRadius/2);
		this.model.selection("anodeSelection").set("xmax",TPCRadius+FSErSpacing);
		this.model.selection("anodeSelection").set("ymin",-electrodeThickness-FSEzSpacing/4);
		this.model.selection("anodeSelection").set("ymax",FSEzSpacing/4);		
	}
	public void makeCathodeSelection(){
		this.model.selection().create("cathodeSelection","Box");
		this.model.selection("cathodeSelection").set("entitydim",1);
		this.model.selection("cathodeSelection").set("xmin",TPCRadius/2);
		this.model.selection("cathodeSelection").set("xmax",TPCRadius+FSErSpacing);
		this.model.selection("cathodeSelection").set("ymin",TPCLength()-FSEzSpacing/4);
		this.model.selection("cathodeSelection").set("ymax",TPCLength()+electrodeThickness+FSEzSpacing/4);		
	}
	public void makeFSESelection(int actualNumber){}//	THIS METHOD DOSES NOT FUNCITON AND MUST BE OVERRIDDEN
	public void makeBoxSelection(String name, double rmin, double zmin, double rmax, double zmax){
		this.model.selection().create(name,"Box");
		this.model.selection(name).set("entitydim",1);
		this.model.selection(name).set("xmin",rmin);
		this.model.selection(name).set("ymin",zmin);
		this.model.selection(name).set("xmax",rmax);
		this.model.selection(name).set("ymax",zmax);
	}
	//public void makeCageSelection(){
		//this.model.selection().create("cageVolumeSelection", "Explicit");
		//this.model.selection("cageVolumeSelection").set(new int[]{2});
		//this.model.selection().create("cageEdgeSelecction", "Adjacent");
		//this.model.selection("cageEdgeSelecction").set("input", new String[]{"cageVolumeSelection"});
	//}
	
	public void makeTerminals(){
		this.model.physics().create("current", "ConductiveMedia", "geom");
		this.model.physics("current").selection().set(new int[] {3});
		this.makeAnodeTerminal();
		for(int i =0; i < FSENumber; i++){
			makeFSETerminal(i);
		}
		this.makeCathodeTerminal();
		//this.makeCageTerminal();
	}
	public void makeAnodeTerminal(){
		this.model.physics("current").feature().create("anodeTerminal", "Ground",1);
		this.model.physics("current").feature("anodeTerminal").selection().named("anodeSelection");		
	}
	public void makeCathodeTerminal(){
		this.model.physics("current").feature().create("cathodeTerminal","Terminal");
		this.model.physics("current").feature("cathodeTerminal").selection().named("cathodeSelection");
		this.model.physics("current").feature("cathodeTerminal").set("TerminalType",1,"Circuit");		
	}
	public void makeFSETerminal(int actualNumber){
		String terminal = "FSE"+actualNumber+"Terminal";
		String selection = "FSE"+actualNumber+"Selection"; 
		this.model.physics("current").feature().create(terminal,"Terminal");
		this.model.physics("current").feature(terminal).selection().named(selection);
		this.model.physics("current").feature(terminal).set("TerminalType",1,"Circuit");
	}
	//public void makeCageTerminal(){
		//this.model.physics("current").feature().create("cageTerminal", "Ground", 1);
		//this.model.physics("current").feature("cageTerminal").selection().named("cageEdgeSelecction");		
	//}
	
	public void makeCircuit(){
		this.model.physics().create("cir", "Circuit", "geom");
		
		this.connectAnode();
		this.connectCathode();
		for(int i = 1; i < FSENumber; i++){
			this.addResistor("Resistor"+i,i+"",i+1+"",Resistance+"[\u03a9]");
			this.addItoU("ItoU"+i,i+1+"","G",i+1);
		}
		this.connectVoltageSource();
	}
	public void connectAnode(){
		this.model.physics("cir").feature("gnd1").set("Connections",1,1,"G");
		this.addResistor("zeroResistor1","0","1","0[\u03a9]");
		this.addItoU("ItoU0","0","G",1);
		this.addResistor("Resistor0","1","G",Resistance+"[\u03a9]");
	}
	public void connectCathode(){
		this.addResistor("zeroResistor2","C1","C2","0[\u03a9]");
		this.addItoU("ItoUC","C1","G",FSENumber+1);
		this.addResistor("Resistor"+FSENumber,FSENumber+"","C2",Resistance+"[\u03a9]");
	}
	public void addResistor(String name, String node1, String node2, String value){
		this.model.physics("cir").feature().create(name,"Resistor",-1);
		this.model.physics("cir").feature(name).set("Connections",1,1,node1);
		this.model.physics("cir").feature(name).set("Connections",2,1,node2);
		this.model.physics("cir").feature(name).set("R",1,value);
	}
	public void addItoU(String name, String node1, String node2, int terminal){
		this.model.physics("cir").feature().create(name, "ModelDeviceIV");
		this.model.physics("cir").feature(name).set("V_src", 1, "root.comp1.ec.V0_"+terminal);
		this.model.physics("cir").feature(name).set("Connections",1,1,node1);
		this.model.physics("cir").feature(name).set("Connections",2,1,node2);
	}
	public void connectVoltageSource(){
		this.model.physics("cir").feature().create("source","VoltageSource",-1);
		this.model.physics("cir").feature("source").set("Connections",1,1,"C2");
		this.model.physics("cir").feature("source").set("Connections",2,1,"G");
		this.model.physics("cir").feature("source").set("value",1,Voltage+"[V]");
	}
	
	public void setMaterials(){
		this.makeCopper();
		this.makeAir(new int[] {1,3});
		}

	public void makeCopper(){
		this.model.material().create("mat1");
	    this.model.material("mat1").name("Copper");
	    this.model.material("mat1").set("family", "copper");
	    this.model.material("mat1").propertyGroup("def").set("relpermeability", "1");
	    this.model.material("mat1").propertyGroup("def")
	         .set("electricconductivity", "5.998e7[S/m]");
	    this.model.material("mat1").propertyGroup("def")
	         .set("thermalexpansioncoefficient", "17e-6[1/K]");
	    this.model.material("mat1").propertyGroup("def")
	         .set("heatcapacity", "385[J/(kg*K)]");
	    this.model.material("mat1").propertyGroup("def").set("relpermittivity", "1");
	    this.model.material("mat1").propertyGroup("def")
	         .set("density", "8700[kg/m^3]");
	    this.model.material("mat1").propertyGroup("def")
	         .set("thermalconductivity", "400[W/(m*K)]");
	    this.model.material("mat1").propertyGroup()
	         .create("Enu", "Young's modulus and Poisson's ratio");
	    this.model.material("mat1").propertyGroup("Enu").set("poissonsratio", "0.35");
	    this.model.material("mat1").propertyGroup("Enu")
	         .set("youngsmodulus", "110e9[Pa]");
	    this.model.material("mat1").propertyGroup()
	         .create("linzRes", "Linearized resistivity");
	    this.model.material("mat1").propertyGroup("linzRes")
	         .set("alpha", "0.0039[1/K]");
	    this.model.material("mat1").propertyGroup("linzRes")
	         .set("rho0", "1.72e-8[ohm*m]");
	    this.model.material("mat1").propertyGroup("linzRes").set("Tref", "298[K]");
	    this.model.material("mat1").set("family", "copper");
	}
	public void makeAir(int[] regions){
	    this.model.material().create("mat2");
	    this.model.material("mat2").name("Air");
	    this.model.material("mat2").set("family", "air");
	    this.model.material("mat2").propertyGroup("def").set("relpermeability", "1");
	    this.model.material("mat2").propertyGroup("def").set("relpermittivity", "1");
	    this.model.material("mat2").propertyGroup("def")
	         .set("dynamicviscosity", "eta(T[1/K])[Pa*s]");
	    this.model.material("mat2").propertyGroup("def")
	         .set("ratioofspecificheat", "1.4");
	    this.model.material("mat2").propertyGroup("def")
	         .set("electricconductivity", "10^-15[S/m]");
	    this.model.material("mat2").propertyGroup("def")
	         .set("heatcapacity", "Cp(T[1/K])[J/(kg*K)]");
	    this.model.material("mat2").propertyGroup("def")
	         .set("density", "rho(pA[1/Pa],T[1/K])[kg/m^3]");
	    this.model.material("mat2").propertyGroup("def")
	         .set("thermalconductivity", "k(T[1/K])[W/(m*K)]");
	    this.model.material("mat2").propertyGroup("def")
	         .set("soundspeed", "cs(T[1/K])[m/s]");
	    this.model.material("mat2").propertyGroup("def").func()
	         .create("eta", "Piecewise");
	    this.model.material("mat2").propertyGroup("def").func("eta")
	         .set("funcname", "eta");
	    this.model.material("mat2").propertyGroup("def").func("eta").set("arg", "T");
	    this.model.material("mat2").propertyGroup("def").func("eta")
	         .set("extrap", "constant");
	    this.model.material("mat2").propertyGroup("def").func("eta")
	         .set("pieces", new String[][]{{"200.0", "1600.0", "-8.38278E-7+8.35717342E-8*T^1-7.69429583E-11*T^2+4.6437266E-14*T^3-1.06585607E-17*T^4"}});
	    this.model.material("mat2").propertyGroup("def").func()
	         .create("Cp", "Piecewise");
	    this.model.material("mat2").propertyGroup("def").func("Cp")
	         .set("funcname", "Cp");
	    this.model.material("mat2").propertyGroup("def").func("Cp").set("arg", "T");
	    this.model.material("mat2").propertyGroup("def").func("Cp")
	         .set("extrap", "constant");
	    this.model.material("mat2").propertyGroup("def").func("Cp")
	         .set("pieces", new String[][]{{"200.0", "1600.0", "1047.63657-0.372589265*T^1+9.45304214E-4*T^2-6.02409443E-7*T^3+1.2858961E-10*T^4"}});
	    this.model.material("mat2").propertyGroup("def").func()
	         .create("rho", "Analytic");
	    this.model.material("mat2").propertyGroup("def").func("rho")
	         .set("funcname", "rho");
	    this.model.material("mat2").propertyGroup("def").func("rho")
	         .set("args", new String[]{"pA", "T"});
	    this.model.material("mat2").propertyGroup("def").func("rho")
	         .set("expr", "pA*0.02897/8.314/T");
	    this.model.material("mat2").propertyGroup("def").func("rho")
	         .set("dermethod", "manual");
	    this.model.material("mat2").propertyGroup("def").func("rho")
	         .set("argders", new String[][]{{"pA", "d(pA*0.02897/8.314/T,pA)"}, {"T", "d(pA*0.02897/8.314/T,T)"}});
	    this.model.material("mat2").propertyGroup("def").func()
	         .create("k", "Piecewise");
	    this.model.material("mat2").propertyGroup("def").func("k")
	         .set("funcname", "k");
	    this.model.material("mat2").propertyGroup("def").func("k").set("arg", "T");
	    this.model.material("mat2").propertyGroup("def").func("k")
	         .set("extrap", "constant");
	    this.model.material("mat2").propertyGroup("def").func("k")
	         .set("pieces", new String[][]{{"200.0", "1600.0", "-0.00227583562+1.15480022E-4*T^1-7.90252856E-8*T^2+4.11702505E-11*T^3-7.43864331E-15*T^4"}});
	    	    this.model.material("mat2").propertyGroup("def").func()
	         .create("cs", "Analytic");
	    this.model.material("mat2").propertyGroup("def").func("cs")
	         .set("funcname", "cs");
	    this.model.material("mat2").propertyGroup("def").func("cs")
	         .set("args", new String[]{"T"});
	    this.model.material("mat2").propertyGroup("def").func("cs")
	         .set("expr", "sqrt(1.4*287*T)");
	    this.model.material("mat2").propertyGroup("def").func("cs")
	         .set("dermethod", "manual");
	    this.model.material("mat2").propertyGroup("def").func("cs")
	         .set("argders", new String[][]{{"T", "d(sqrt(1.4*287*T),T)"}});
	    this.model.material("mat2").propertyGroup("def").addInput("temperature");
	    this.model.material("mat2").propertyGroup("def").addInput("pressure");
	    this.model.material("mat2").propertyGroup()
	         .create("RefractiveIndex", "Refractive index");
	    this.model.material("mat2").propertyGroup("RefractiveIndex").set("n", "1");
	    this.model.material("mat2").set("family", "air");
	    this.model.material("mat2").selection().set(regions);
	}
	
	public void export(String file){
		this.makeDataSet();
		
	}
	public void makeDataSet(){
	    this.model.result().dataset().create("cpt1", "CutPoint2D");
	    this.model.result().dataset("cpt1").set("method", "grid");
	    this.model.result().dataset("cpt1").set("gridx", "range(0,1,359)");
	    this.model.result().dataset("cpt1").set("gridy", "range(0,1,"+this.TPCLength()+")");
	}

	public void makeSolver(){
	    this.model.study().create("study");
	    this.model.study("study").feature().create("solver", "Stationary");
	    this.model.study("study").feature("solver").activate("current", true);
	    this.model.study("study").feature("solver").activate("cir", true);
	}
}