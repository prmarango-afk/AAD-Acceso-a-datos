import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        try {
            File myObj = new File("reservas.txt");
            if(myObj.createNewFile()){
                System.out.println("File created: " + myObj.getName());

            }else {
                System.out.println("File already exists.");
            }
        }catch(IOException e){
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    };

    Person juanjo = new Person("46525865R", "");

    try(BufferedReader br = new BufferedReader(new FileReader("filename.txt")){
        String line;
        while((line = br readLine()))
    }
}
