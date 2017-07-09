/* Guillermo Alicea
** 11/21/2016
** CAP 4630 - Fall 2016 - Professor Glinos
** Program 3 - Perceptron Classifier
** -- Appears to work with (simple/diabetes/ionosphere/iris/
** segment-challene/segment-test/weather.nominal/weather.numeric).arff --
*/

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.Arrays;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.CapabilitiesHandler;
import weka.core.Instance;
import weka.core.Instances;

public class Perceptron implements weka.classifiers.Classifier, CapabilitiesHandler{

	private static final DecimalFormat DF = new DecimalFormat("0.000");
	
	public Instances data;
	
	public double[][] input;
	
	public double[][] classification;
	
	public int epochs;
	
	public double learningRate;
	
	public double[] weights;
	
	public int updateCount = 0;
	
	public String fileName;
	
	Perceptron(String[] options) throws Exception
	{
		fileName = options[0];
		
		data = new Instances(new BufferedReader(new FileReader(options[0])));
		data.setClassIndex(data.numAttributes() - 1);
		data.deleteWithMissingClass();
		
		weights = new double[data.numAttributes()];
		
		epochs = Integer.parseInt(options[1]);
		
		learningRate = Double.parseDouble(options[2]);
		
		classification = new double[data.numInstances()][data.numAttributes()];
		
		input = new double[data.numInstances()][data.numAttributes() + 1];
		
		//input[][] can take the task of classification and erase the need
		//for the extra memory, but this works all the same and, i think, 
		//shows more clearly that this array will be used later to determine
		//the class of each instance
		for (int i = 0; i < classification.length; i++)
			for (int j = 0; j < classification[i].length; j++)
				classification[i][j] = data.get(i).value(j);
		
		for (int i = 0; i < input.length; i++)
			for (int j = 1; j < input[i].length; j++)
			{	
				input[i][0] = 1;
				input[i][j] = data.get(i).value(j - 1);
			}
	}
	
	@Override
	public void buildClassifier(Instances data) throws Exception 
	{
		
		//header
		System.out.println("\nUniversity of Central Florida");
		System.out.println("CAP4630 Artificial Intelligence - Fall 2016");
		System.out.println("Perceptron Classifier by Guillermo Alicea\n");
		
		int[] report = new int[data.numInstances()];
		
		//initialize with 0
		for (int i = 0; i < data.numAttributes(); i++)
			weights[i] = 0.0;
		
		//Perceptron classifier loop
		for (int i = 0; i < epochs; i++)
		{
			for (int j = 0; j < data.numInstances(); j++)
			{
				int error = 0;
				
				//f(sum(weights, x's))
				int fNet = computeFNet(input[j], weights);
				
				//This looks incorrect at first glance, but, to my understanding, 
				//weka orders classes by order (i.e. {1, 0} => 1 = 0, 0 = 1), so
				//we adjust our conditions to work with these considerations
				if (fNet != classification[j][data.classIndex()])
					error = 0;
				else if (fNet == 0 && classification[j][data.classIndex()] == 0)
					error = 2;
				else
					error = -2;
				
				//update report and updateCount if necessary
				if (error == 0)
					report[j] = 1;
				else
				{
					updateCount++;
					report[j] = 0;
				}
				
				//weight updater
				for (int k = 0; k < data.numAttributes(); k++)
				{
					double delta = learningRate * input[j][k] * error;
					
					weights[k] += delta;
				}
			}
			
			//report epoch results
			System.out.print("Epoch   " + i + ": ");
			for (int j = 0; j < report.length; j++)
				System.out.print(report[j]);
			System.out.print("\n");
		}
		
	}
	
	//compute f(sum(weights, x's))
	public int computeFNet(double[] instance, double[] weights)
	{
		double sum = 0.0;
		
		for (int i = 0; i < data.numAttributes(); i++)
		{
			sum += weights[i] * instance[i];
		}
		
		if (sum >= 0)
			return 1;
		return 0;
	}

	@Override
	public double classifyInstance(Instance arg0) throws Exception 
	{
		return 0;
	}

	@Override
	public double[] distributionForInstance(Instance inst) throws Exception 
	{
		
		double[] dist = new double[data.numClasses()];
		
		int i = 0, j = 0;
		for (i = 0; i < classification.length; i++)
		{
			if (inst.value(0) == classification[i][0])
			{
				for (j = 0; j < classification[i].length - 1; j++)
					if (inst.value(j) != classification[i][j])
						break;
			}
			if (j >= classification[i].length - 1)
				break;
		}
		
		dist[(int) classification[i][j]] = 1;
				
		return dist;
	}

	//recommended in the weka manual (not sure what it's for)
	@Override
	public Capabilities getCapabilities() 
	{
		// TODO Auto-generated method stub
		Capabilities result = new Capabilities(this);
		// attributes
		result.enable(Capability.NOMINAL_ATTRIBUTES);
		result.enable(Capability.NUMERIC_ATTRIBUTES);
		// class
		result.enable(Capability.NUMERIC_CLASS);
		return result;
	}

	//returns String which contains our output results
	public String toString()
	{
		String model;
		
		model = "Source file: " + fileName + "\nTraining epochs: " + epochs + 
				"\nLearning rate: " + learningRate;
		
		model += "\n\nTotal # weight updates = " + updateCount + "\nFinal weights:\n";
		
		Arrays.sort(weights);
		
		for (int i = 0; i < weights.length; i++)
		{
			model += String.valueOf(DF.format(weights[i]));
			model += "\n";
		}
		
		return model;
	}
}
