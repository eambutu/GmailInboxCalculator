package sample;

/**
 * Created by Phillip on 5/26/2015.
 */
import java.io.*;
import java.util.*;
import com.google.api.services.gmail.model.Message;
public class MatrixCalculator {
    public static double[][] matrixProduct(double[][] a,double[][] b){
        if(a[0].length!=b.length) System.out.println("Product has wrong dimensions");
        double[][] c = new double[a.length][b[0].length];
        for(int i=0;i<a.length;i++){
            for(int j=0;j<b[0].length;j++){
                double sum = 0;
                for(int k=0;k<b.length;k++){
                    sum+=a[i][k]*b[k][j];
                }
                c[i][j] = sum;
            }
        }
        return c;
    }
    public static double[] findImportance(double[][] a){
        double[][] temp = new double[a.length][1];
        for(int i=0;i<a[0].length;i++){
            temp[i][0] = 1;
        }
        for(int i=0;i<100;i++){ //change the constant "100" depending on how fast it converges
            double[][] res = temp;
            temp = matrixProduct(a,temp); //Double.MAX_VALUE somewhere around 10^308, shouldnt overflow (hopefully)
            if(isParallel(res,temp)) break;
        }
        double[] result = new double[a[0].length];
        double sum = 0;
        for(int i=0;i<a[0].length;i++){
            sum += temp[i][0];
        }
        for(int i=0;i<a[0].length;i++){
            result[i] = temp[i][0]*100/sum;
        }
        return result;
    }

    public static boolean isParallel(double[][] a, double[][] b){
        if(!(a.length==b.length&&a[0].length==b[0].length)) System.out.println("Parallel on different dimension matrices");
        boolean parallel = true;
        for(int i=0;i<a.length;i++){
            for(int j=0;j<a[0].length;j++){
                if(!(Math.abs(((double)b[i][j]/(double)a[i][j])-((double)b[0][0]/(double)a[0][0]))<1)){ //change the constant "1" based off what is "parallel"
                    parallel = false;
                    break;
                }
            }
        }
        return parallel;
    }
}
