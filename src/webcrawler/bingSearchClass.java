/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webcrawler;

/**
 *
 * @author pradeep
 */

import java.util.*;
import java.net.*;

import org.jsoup.*;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class bingSearchClass {
    static ArrayList<String> yahooResults ;       //storing results from google search.
    
    static int count = 0;
    
    public static String search() throws Exception 
    {
        yahooResults = new ArrayList<>() ;
        
        Scanner sc = new Scanner(System.in);
        
        System.out.print("Enter String to search: ");
        
        String search = sc.nextLine() ;
        
        if( count == 0 )
        {
            System.out.println("\nNo result found.\nExiting.....");
            System.exit(0);
        }
        
        System.out.print("\n\nSelect one out of "+ count +" results : ");
        
        int choice = sc.nextInt() ;
        
        if( choice > count || choice < 1 )
        {
            System.out.println("Invalid choice . Default First result.");
            choice = 1 ;
        }
        
        return yahooResults.get(choice-1) ;
    }
}