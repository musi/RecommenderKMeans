package netflix.memreader;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import netflix.utilities.Pair;
import netflix.utilities.Triple;

import cern.colt.function.IntObjectProcedure;		//from where these packages comes from?
import cern.colt.list.IntArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.list.ObjectArrayList;
import cern.colt.map.OpenIntDoubleHashMap;
import cern.colt.map.OpenIntIntHashMap;
import cern.colt.map.OpenIntObjectHashMap;

/***********************************************************************************************************************/

/**
 * MemHelper provides all the methods for retrieving, parsing,
 * and joining data together from a MemReader object.
 *
 * @author Dan Lew
 * @author Amrit Tuladhar
 * @author Ben Sowell
 */

/** 
 * Modified by 
 * @author Musi
 */
/***********************************************************************************************************************/

public class MemHelper 
{
    // The "database" for this MemHelper
    private MemReader mr;

    private OpenIntObjectHashMap midToName;
    private OpenIntObjectHashMap midToAttributes;		//to be used for CBF
    private OpenIntObjectHashMap uidToAttributes;
    private OpenIntObjectHashMap midToGenres;			//to be used for Demographic correlation
    

 /***********************************************************************************************************************/
    
    /**
     * Constructs a new MemHelper that uses the specified MemReader for data
     * @param mr the MemReader that holds all the data
     */

        
    public MemHelper(MemReader mr)   //with memreader object passed    
    {
        this.mr 	= mr;
        midToName 	= null;
    }

 /***********************************************************************************************************************/
    
    /**
     * Constructs a new MemHelper by loading the serialized
     * MemReader from the specified file. 
     *
     * @param  fileName  The file containing serialized MemReader object
     */
    
       
    public MemHelper(String fileName) 				//with written object's path passed    
    {
        mr = MemReader.deserialize(fileName);		//deserialize from a file (we have written a memreader object there)
        midToName = null;
    }

 /***********************************************************************************************************************/
    
    /**
     * Returns the MemReader object maintained 
     * by this MemHelper. 
     *
     * @return  The MemReader object managed by
     *          this MemHelper.
     */
    
       
     public MemReader getMemReader()      
     {
        return mr;
     }

  /***********************************************************************************************************************/
     
     /**
     * Returns the customer to movie hash table. 
     *
     * @return  The custToMovie hash table. 
     */
     
    public OpenIntObjectHashMap getCustToMovie()    
    {
        return mr.custToMovie;
    }

  /***********************************************************************************************************************/
    
    /**
     * Returns the movie to customer hash table. 
     *
     * @return  The movieToCust hash table. 
     */
  
      
    public OpenIntObjectHashMap getMovieToCust()    
    {
        return mr.movieToCust;
    }
    
 /***********************************************************************************************************************/
    
    /**
     * Returns the movie to Genre hash table. 
     *
     * @return  The movieToGenre hash table. 
     */
  
      
    public OpenIntObjectHashMap getMovieToGenre()    
    {
        return mr.movieToGenre;
    }
    
  /***********************************************************************************************************************/
    
    /**
     * Returns the sumByCust hash table. 
     *
     * @return  The custToMovie hash table. 
     */
    
    public OpenIntDoubleHashMap getSumByCust()    
    {
        return mr.sumByCust;
    }

  /***********************************************************************************************************************/
    
    /**
     * Returns the sumByMovie hash table. 
     *
     * @return  The custToMovie hash table. 
     */
    
    public OpenIntDoubleHashMap getSumByMovie()    
    {
        return mr.sumByMovie;
    }

  /***********************************************************************************************************************/
    
    //How we apply these procedures to each pair and what this procedure means?
    
    /**
     * Applies the specified IntObjectProcedure to each key, value pair
     * in the custToMovie hash table. 
     *
     * @param  procedure  The IntObjectProcedure to appy to
     *                    the custToMovie hash table. 
     * @return  True if the apply function returned true. 
     */
 
     
    public boolean applyToUserPairs(IntObjectProcedure procedure)    
    {
        return mr.custToMovie.forEachPair(procedure);				
    }

  /***********************************************************************************************************************/    
    
    /**
     * Applies the specified IntObjectProcedure to each key, value pair
     * in the movieToCust hash table. 
     *
     * @param   procedure  The IntObjectProcedure to appy to
     *                    the movieToCust hash table. 
     * @return  True if the apply function returned true. 
     */
    
    public boolean applyToMoviePairs(IntObjectProcedure procedure)    
    {
        return mr.movieToCust.forEachPair(procedure);
    }

  /***********************************************************************************************************************/
    
    
    /**
     * Returns the rating portion of a uid/rating or mid/rating block
     *
     * @param block a uid/rating or mid/rating block
     * @return the rating
     */
    
    public static double parseRating(long block)    
    {
        long mask = 0x000000000000FFFFL;
        short dumR = (short)(block & mask);
        
        return (dumR*1.0/100);	//as while storing in MemReader, we stored the (rating*100) with cast in short 
        
    }

 /***********************************************************************************************************************/
    
    /**
     * Returns the uid or mid portion of a uid/rating or mid/rating block
     *
     * @param block a uid/rating or mid/rating block
     * @return the uid or mid
     */
    public static int parseUserOrMovie(long block)     
    {
    	 long mask = 0xFFFFFFFFFFFF0000L;
         return (int)((block & mask)>>16);
    }

/***********************************************************************************************************************/    
    /**
     * Returns the rating that the user gave the movie
     * 
     * @param uid the user id
     * @param mid the movie id
     * @return the rating
     */
    
    //As in moviesToCust, we have cid packed with rating, so to check whether a user have seen that movie 
    //we need custToMovie as well
    
    public double getRating(int uid, int mid)     
    {
    	if (mr.movieToCustRating.containsKey(mid) && mr.custToMovieRating.containsKey(uid))    		
    	{
    		// These movies are rated by the following users in the training set
    		// Get all users who have rated this movie
    		  OpenIntDoubleHashMap myMap= (OpenIntDoubleHashMap)mr.movieToCustRating.get(mid);    		
    		
    		// check if the active user (uid) has rated the movie (mid)
    		// If it map.get ==0, user has not rated this    		  
    		  double rating = myMap.get(uid);
    		
    		if(rating == 0.0) 
    			return -99;
    		    		
    		return (rating); 

    		
    		
    		
    		//this was errored version 
    		/*OpenIntDoubleHashMap myMap= (OpenIntDoubleHashMap)mr.movieToCustRating.get(mid);
    		return (myMap.get(uid));
    		*/
    		
    	}
    		
      /*  if (mr.movieToCust.containsKey(mid) && mr.custToMovie.containsKey(uid))        
        {
        	
        	LongArrayList custList  = (LongArrayList) mr.movieToCust.get(mid);
        	LongArrayList movieList = (LongArrayList) mr.custToMovie.get(uid);
			
        	       	
            // Binary search through a list, assuming:
            // 1. The list is already sorted
            // 2. Ratings are limited to 1-5
            long myId = mid;
           if (custList.size() > movieList.size())				     //search the smallest one
            {
                long tempmid = myId <<16;							//mid left shift 16 = some constant 
                
                //MovieList is a list of movies (consists of mid+Ratings) against a uid 
                //so sach a speicific mid from this list using a ++(mid<<8)
                
                for (int i=1; i<=1000; i++) //my max ratin is 10 *100 =100
                    if (movieList.binarySearch(++tempmid) >= 0)	//some constant + (1,2,3,4,5)	
                    { //System.out.println("ratingM----->"+ (i*1.0/100));  
                      return i*1.0/100;
                    
                     }
              }
                     
           else            
             {    
                long tempuid = uid<<16 ;
                
                //CustList is a list of users (consists of uid+Ratings) who saw movie (mid)
                //search for a uid from this list using ++(uid<<8)
                for (int i=1; i<=1000; i++)
                    if (custList.binarySearch(++tempuid) >= 0)
                      { //System.out.println("rating-->"+ (i*1.0/100));  
                         return i*1.0/100;}
            }
        
            
           }//end of if
        */
    		
    		
  /*      else {
        	
        	//System.out.println("not there -->" + uid + "," +mid);
        	
        	if (mr.movieToCust.containsKey(mid) && mr.custToMovie.containsKey(uid))
        		System.out.println("but there -->" + uid + "," +mid);
        }*/
         
        // Not found, return default value
        //System.out.println("rating---->-99" +"," + uid + "," +mid);
        return -99;
        
    }

/***********************************************************************************************************************/    

    /**
     * Returns the average rating for a particular movie
     *
     * @param mid the movie id
     * @return the average rating for the movie
     */
    
    public double getAverageRatingForMovie(int mid)     
    {
        double avg = (double) mr.sumByMovie.get(mid) 
                      / (double) getNumberOfUsersWhoSawMovie(mid);

        if(Double.isNaN(avg))
            return 0.0;
        else 
            return avg;
    }

    
 /***********************************************************************************************************************/
     
    /**
     * Returns the average rating for a particular user
     *
     * @param uid the user id
     * @return the average rating for the user
     */
    
    public double getAverageRatingForUser(int uid)     
    {

        double avg = (double) mr.sumByCust.get(uid) 
                    / (double)getNumberOfMoviesSeen(uid);

        if(Double.isNaN(avg))
            return 0.0;
        else 
            return avg;
    }
 
 /***********************************************************************************************************************/
        
       /**
        * Returns the min/max rating for a particular user
        *
        * @param uid the user id
        * @param min, true for min rating, false for max rating
        * @return the min/max rating for the user
        */
       
       public double getMinRatingForUser(int uid, boolean min)     
       {

    	   LongArrayList mov =getMoviesSeenByUser(uid);
    	   int size = mov.size();
    	   double minRating =15;
    	   double maxRating =0;    	   
    	   
    	   for(int i=0;i<size;i++)
    	   {
    		   int mid	= parseUserOrMovie(mov.getQuick(i));
    		   double rat = getRating(uid, mid);
    		  
    		   if(minRating > rat )
    			   minRating = rat;
    		   if(maxRating<rat)
    			   maxRating =rat;
    	   }
    	   
    	   if(min==true)
    		   return minRating;
    	   else 
    		   return maxRating;
       }
    
       
/***********************************************************************************************************************/    

       
       /**
        * Returns the min/max rating for a particular Mov
        *
        * @param mid the mov id
        * @param min, true for min rating, false for max rating
        * @return the min/max rating for the Mov
        */
       
       public double getMinRatingForMovie(int mid, boolean min)     
       {
    	   LongArrayList users = getUsersWhoSawMovie(mid);
    	   int size = users.size();
    	   double minRating =15;
    	   double maxRating =0;    	   
    	       
    	   for(int i=0;i<size;i++)
    	   {
    		   int uid	= parseUserOrMovie(users.getQuick(i));
    		   double rat = getRating(uid, mid);
    		  
    		   if(minRating > rat )
    			   minRating = rat;
    		   if(maxRating<rat)
    			   maxRating =rat;
    	   }
    	   
    	   if(min==true)
    		   return minRating;
    	   else 
    		   return maxRating;
                   
       }
        
    
 /***********************************************************************************************************************/
    
    /**
     * Calculates the standard deviation for a particular user
     * @param uid the user id
     * @return the user's standard deviation
     */
    
    // why dividing by size-1?
    // movies.size() = Returns the number of elements contained in the receiver
    // for non-group data, it divide it by n-1
    
    public double getStandardDeviationForUser(int uid)     
    {
        double avg = getAverageRatingForUser(uid), sd = 0;
        
        LongArrayList movies = getMoviesSeenByUser(uid);
        
        for(int i = 0; i < movies.size(); i++)
            sd += Math.pow((double)parseRating(movies.getQuick(i)) - avg, 2); //d1=sum (sq(r-avgr))
            
        if(movies.size() == 1)
           return Math.sqrt(sd);
        
        else //why (size-1), as size returns the number of elements (WIKIPEDIA say it)
           return Math.sqrt(sd / (movies.size() - 1.0)); // d= sqrt(d1/N) 
    }
    
/***********************************************************************************************************************/    

    /**
     * Calculates the standard deviation for a particular movie
     * @param mid the movie id
     * @return the movie's standard deviation
     */
    public double getStandardDeviationForMovie(int mid)     
    {
        double avg = getAverageRatingForMovie(mid), sd = 0;
        
        LongArrayList users = getUsersWhoSawMovie(mid);
        
        for(int i = 0; i < users.size(); i++)
            sd += Math.pow((double)parseRating(users.getQuick(i)) - avg, 2);
            
        if(users.size() == 1)
            return Math.sqrt(sd);
        else
            return Math.sqrt(sd / (users.size() - 1.0));        
    }
    
/***********************************************************************************************************************/   

    /**
     * Returns the sum of the ratings for a 
     * particular user. This is useful for computing
     * the average rating of an arbitrary subset of 
     * users.
     *
     * @param  uid The user id
     * @return The average rating for the user. 
     */
    
    public double getRatingSumForUser(int uid)     
    {   
        return mr.sumByCust.get(uid);
    }
    
 /***********************************************************************************************************************/   

    /**
     * Returns the sum of the ratings for a 
     * particular movie. This is useful for computing
     * the average rating of an arbitrary subset of 
     * movies.
     *
     * @param  mid The movie id
     * @return The average rating for the movie. 
     */
    
    public double getRatingSumForMovie(int mid)     
    {   
        return mr.sumByMovie.get(mid);
    }
    
/***********************************************************************************************************************/   

    /**
     * Returns the average rating in the dataset. 
     *
     * @return The average rating in the dataset. 
     */
    
    public double getGlobalAverage() //> what that means and how it is calculating it?    
    {
        IntArrayList users = getListOfUsers();   //It retrurn all the users associated with all movies (means all users in the db)
        
        double sum = 0.0;
        int count = 0;

        for(int i = 0; i < users.size(); i++) 
        
        {
            sum += getRatingSumForUser(users.get(i));         
            count += getNumberOfMoviesSeen(users.get(i));	 
        }
        
        return sum/count;
    }


 /***********************************************************************************************************************/   

        /**
         * Returns the avg rating given to movies
         *
         * @return The avg rating given to movies  
         */
        
        public double getGlobalMovAverage() //> what that means and how it is calculating it?    
        {
            IntArrayList movies = getListOfMovies();   //It retrurn all the users associated with all movies (means all users in the db)
            
            double sum = 0.0;
            int count = 0;

            for(int i = 0; i < movies.size(); i++)            
            {
                sum += getRatingSumForMovie(movies.get(i));
                count += getNumberOfUsersWhoSawMovie(movies.get(i));	 
            }
            
            return sum/count;
        }
    
  /***********************************************************************************************************************/
    
    /**
     * Returns total rating in the dataset. 
     *
     * @return The total number of rating in the dataset. 
     */
    
    public double getAllRatingsInDB() //> what that means and how it is calculating it?    
    {
        IntArrayList users = getListOfUsers();   //It retrurn all the users associated with all movies (means all users in the db)
        
        int count = 0;

        for(int i = 0; i < users.size(); i++) 
        
        {
            count += getNumberOfMoviesSeen(users.get(i)); //simply count all the movies seen by all users and add them		 
        }
        
        return count;
    }
  
 /***********************************************************************************************************************/    
    
    /**
     * Returns the list of all mid/rating blocks for movies
     *
     * @return the list of all mid/rating blocks for movies
     */
    
    public IntArrayList getListOfMovies()     
    {
        return mr.movieToCust.keys();
    }


  /***********************************************************************************************************************/
    
    /**
     * Returns the list of all movies against a genre
     *
     * @return the list of all genres for movies
     */
    
    public IntArrayList getListOfMoviesAgainstAGenre()    
    {
        return mr.movieToGenre.keys();
    }

 /***********************************************************************************************************************/
    
    /**
     * Returns the list of all uid/rating blocks for users
     *
     * @return the list of all uid/rating blocks for users
     */
    public IntArrayList getListOfUsers()     
     {
    	
    //	System.out.println("Inside getListOfUsers:");
    //	System.out.println("Size of it is" + mr.custToMovie.keys().size());
    
    	return mr.custToMovie.keys();
    }

 /***********************************************************************************************************************/    
 
    /**
     * Returns the number of movies
     *
     * @return the number of movies
     */
    public int getNumberOfMovies()    
    {
        return mr.movieToCust.size();
    }

 /***********************************************************************************************************************/    
 
    /**
     * Returns the number of users (All the key associations)
     *
     * @return the number of users
     */
    public int getNumberOfUsers()     
    {
    	////Returns the number of (key,value) associations currently contained.
    //	System.out.println("Inside getListOfUsers:");
   // 	System.out.println("Size of it is" + mr.custToMovie.keys().size());
       	 
    	return mr.custToMovie.size();
    }

/***********************************************************************************************************************/   

    /**
     * Returns the number of users who saw a particular movie
     *
     * @param mid the movie id
     * @return the number of users who saw the movie
     */
    public int getNumberOfUsersWhoSawMovie(int mid)    
    {
        if (mr.movieToCust.containsKey(mid))        
        {
            return ((LongArrayList)mr.movieToCust.get(mid)).size();
        }
        
        return 0;
    }

/***********************************************************************************************************************/
    
    /**
     * Returns the number of Genres against a movie
     *
     * @param mid the movie id
     * @return the number of genres that lies under that movie
     */
 
    public int getNumberOfGenresAgainstAMovie(int mid)    
    {
        if (mr.movieToGenre.containsKey(mid))        
        {
            return ((IntArrayList)mr.movieToGenre.get(mid)).size();
        }
        
        return 0;
    }

/***********************************************************************************************************************/   

    /**
     * Returns the number of movies seen by a particular user
     *
     * @param uid the user id
     * @return the number of movies seen by the user
     */
    
    public int getNumberOfMoviesSeen(int uid)    
    {
        if (mr.custToMovie.containsKey(uid))        
        {
            return ((LongArrayList)mr.custToMovie.get(uid)).size();
        }
     
        return 0;
    }
 
 /***********************************************************************************************************************/
    
    
    
    /**
     * Returns all users/ratings who have seen a particular movie.
     * 
     * It is returned as an array of uid/rating blocks.
     * @param mid the movie id
     * @return an array of uid/rating blocks
     */
    
    public LongArrayList getUsersWhoSawMovie(int mid)     
    {
        if (mr.movieToCust.containsKey(mid))         
        {
            return (LongArrayList) mr.movieToCust.get(mid);
        }

        return new LongArrayList(); //fucking, it is sending uninitailzed array if not found
    }

 /***********************************************************************************************************************/
    
    /**
     * Returns all Genres against a movie
     * 
     * It is returned as an array of genres.
     * @param mid the movie id.
     * @return an array of genres.
     */
    
    public LongArrayList getGenreAgainstAMovie(int mid)     
    {
    	   	
        if (mr.movieToGenre.containsKey(mid))         
        {
        	//System.out.println(" Inside, genre(mid).size =" + ((IntArrayList)(mr.movieToGenre.get(mid))).size()); 
            return (LongArrayList) mr.movieToGenre.get(mid);
        }
        
        //System.out.println("Outside, mid not found =" + mid );
        return new LongArrayList(); 	
    }


 /***********************************************************************************************************************/
 /***********************************************************************************************************************/
    
    /**
     * Returns all keywords against a movie
     * 
     * It is returned as an array of keywords.
     * @param mid the movie id.
     * @return an array of keywords.
     */
    
    public HashMap<String,Double> getKeywordsAgainstAMovie(int mid)     
    {
    	
    	//System.out.println("keyword method called in memHelper");
    	
        if (mr.movieToKeywords.containsKey(mid))         
        {
        //	System.out.println("keyword has some size");	 
            return (HashMap<String,Double>) mr.movieToKeywords.get(mid);
        }
                
        return new HashMap<String,Double>(); 	
    }


 //-------------------------------------------------------------------------------
    
    /**
     * Returns all features against a movie
     * 
     * It is returned as an array of features.
     * @param mid the movie id.
     * @return an array of features.
     */
    
    public HashMap<String,Double> getFeaturesAgainstAMovie(int mid)     
    {
    	
        if (mr.movieToFeatures.containsKey(mid))         
        {
        //	System.out.println("keyword has some size");	 
            return (HashMap<String,Double>) mr.movieToFeatures.get(mid);
        }
                
        return new HashMap<String,Double>(); 	
    }
    
    //-------------------------------------------------------------------------------
    
    /**
     * Returns all features against a movie
     * 
     * It is returned as an array of features.
     * @param mid the movie id.
     * @return an array of features.
     */
    
    public HashMap<String,Double> getUnStemmedFeaturesAgainstAMovie(int mid)     
    {
    	
        if (mr.movieToUnStemmedFeatures.containsKey(mid))         
        {
        //	System.out.println("keyword has some size");	 
            return (HashMap<String,Double>) mr.movieToUnStemmedFeatures.get(mid);
        }
                
        return new HashMap<String,Double>(); 	
    }
    
    
    //-------------------------------------------------------------------------------
    
    /**
     * Returns all non-matching movies
     * 
     * @return an array of ids for movies not exactly matched.
     */
    
    public IntArrayList getNonMatchingMovies()     
    {
    	return mr.moviesNotMatched;
      	
    }
    
  //-------------------------------------------------------------------------------
    
       /**
        * Returns all Colors Info against a movie
        * 
        * It is returned as an array of keywords.
        * @param mid the movie id.
        * @return an array of keywords.
        */
       
       public HashMap<String,Double> getColorsAgainstAMovie(int mid)     
       {
       	
       	//System.out.println("keyword method called in memHelper");
       	
           if (mr.movieToColors.containsKey(mid))         
           {
           //	System.out.println("keyword has some size");	 
               return (HashMap<String,Double>) mr.movieToColors.get(mid);
           }
                   
           return new HashMap<String,Double>(); 	
       }
       

   //-------------------------------------------------------------------------------      


      /**
        * Returns all Tags against a movie
        * 
        * It is returned as an array of tags.
        * @param mid the movie id.
        * @return an array of tags.
        */
       
       public HashMap<String,Double> getTagsAgainstAMovie(int mid)     
       {
       	   	
           if (mr.movieToTags.containsKey(mid))         
           {
           	 
               return (HashMap<String,Double>) mr.movieToTags.get(mid);
           }
                      
           return new HashMap<String,Double>(); 	
       }
         


       //-------------------------------------------------------------------------------      


          /**
            * Returns Language info against a movie
            * 
            * It is returned as an array of tags.
            * @param mid the movie id.
            * @return an array of tags.
            */
           
           public HashMap<String,Double> getLanguageAgainstAMovie(int mid)     
           {
           	   	
               if (mr.movieToLanguages.containsKey(mid))         
               {
               	  return (HashMap<String,Double>) mr.movieToLanguages.get(mid);
               }
                          
               return new HashMap<String,Double>(); 	
           }

       //-------------------------------------------------------------------------------
           
           /**
             * Returns all Tags against a movie
             * 
             * It is returned as an array of tags.
             * @param mid the movie id.
             * @return an array of tags.
             */
            
            public HashMap<String,Double> getCertificateAgainstAMovie(int mid)     
            {
            	   	
                if (mr.movieToCertificates.containsKey(mid))         
                {                	 
                    return (HashMap<String,Double>) mr.movieToCertificates.get(mid);
                }
                           
                return new HashMap<String,Double>(); 	
            }

          //-------------------------------------------------------------------------------
            
            /**
              * Returns Biography against a movie
              * 
              * It is returned as an array of tags.
              * @param mid the movie id.
              * @return an array of tags.
              */
             
             public HashMap<String,Double> getBiographyAgainstAMovie(int mid)     
             {
             	   	
                 if (mr.movieToBiography.containsKey(mid))         
                 {                	 
                     return (HashMap<String,Double>) mr.movieToBiography.get(mid);
                 }
                            
                 return new HashMap<String,Double>(); 	
             }

  
          //-------------------------------------------------------------------------------
             
             /**
               * Returns  Printed Review against a movie
               * 
               * It is returned as an array of tags.
               * @param mid the movie id.
               * @return an array of tags.
               */
              
           public HashMap<String,Double> getPrintedReviewsAgainstAMovie(int mid)     
           {
              	   	
               if (mr.movieToPrintedReviews.containsKey(mid))         
               {                	 
                    return (HashMap<String,Double>) mr.movieToPrintedReviews.get(mid);
               }
                             
                return new HashMap<String,Double>(); 	
           }


        //-------------------------------------------------------------------------------
           
           /**
             * Returns Plots against a movie
             * 
             * It is returned as an array of tags.
             * @param mid the movie id.
             * @return an array of tags.
             */
            
         public HashMap<String,Double> getPlotsAgainstAMovie(int mid)     
         {
            	   	
             if (mr.movieToPlots.containsKey(mid))         
             {                	 
                  return (HashMap<String,Double>) mr.movieToPlots.get(mid);
             }
                           
              return new HashMap<String,Double>(); 	
         }

 

       //-------------------------------------------------------------------------------
           
           /**
             * Returns Votes against a movie
             * 
             * It is returned as an array of tags.
             * @param mid the movie id.
             * @return an array of tags.
             */
            
            public HashMap<String,Double> getVotesAgainstAMovie(int mid)     
            {
            	   	
                if (mr.movieToVotes.containsKey(mid))         
                {                	 
                    return (HashMap<String,Double>) mr.movieToVotes.get(mid);
                }
                           
                return new HashMap<String,Double>(); 	
            }


            
            //-------------------------------------------------------------------------------
                
                /**
                  * Returns Ratings against a movie
                  * 
                  * It is returned as an array of tags.
                  * @param mid the movie id.
                  * @return an array of tags.
                  */
                 
                 public HashMap<String,Double> getRatingsAgainstAMovie(int mid)     
                 {
                 	   	
                     if (mr.movieToRatings.containsKey(mid))         
                     {                	 
                         return (HashMap<String,Double>) mr.movieToRatings.get(mid);
                     }
                                
                     return new HashMap<String,Double>(); 	
                 }
 
                 //-------------------------------------------------------------------------------
                 
                 /**
                   * Returns Actors against a movie
                   * 
                   * It is returned as an array of Actors ids.
                   * @param mid the movie id.
                   * @return an array of tags.
                   */
                  
                  public HashMap<String,Double> getActorsAgainstAMovie(int mid)     
                  {
                  	   	
                      if (mr.movieToActors.containsKey(mid))         
                      {                	 
                          return (HashMap<String,Double>) mr.movieToActors.get(mid);
                      }
                                 
                      return new HashMap<String,Double>(); 	
                  }
                  
                  //-------------------------------------------------------------------------------
                  
                  /**
                    * Returns Directors against a movie
                    * 
                    * It is returned as an array of Directors ids.
                    * @param mid the movie id.
                    * @return an array of tags.
                    */
                   
                   public HashMap<String,Double> getDirectorsAgainstAMovie(int mid)     
                   {
                   	   	
                       if (mr.movieToDirectors.containsKey(mid))         
                       {                	 
                           return (HashMap<String,Double>) mr.movieToDirectors.get(mid);
                       }
                                  
                       return new HashMap<String,Double>(); 	
                   }
                   
                 //-------------------------------------------------------------------------------
                   /**
                    * Returns Genres against a movie
                    * 
                    * It is returned as an array of Genres.
                    * @param mid the movie id.
                    * @return an array of tags.
                    */
                   
                   public HashMap<String,Double> getGenresAgainstAMovie(int mid)     
                   {
                   	   	
                       if (mr.movieToGenres.containsKey(mid))         
                       {                	 
                           return (HashMap<String,Double>) mr.movieToGenres.get(mid);
                       }
                                  
                       return new HashMap<String,Double>(); 	
                   }
                                      
                   //-------------------------------------------------------------------------------
                   
                   /**
                     * Returns Producers against a movie
                     * 
                     * It is returned as an array of Producers ids.
                     * @param mid the movie id.
                     * @return an array of tags.
                     */
                    
                    public HashMap<String,Double> getProducersAgainstAMovie(int mid)     
                    {
                    	   	
                        if (mr.movieToProducers.containsKey(mid))         
                        {                	 
                            return (HashMap<String,Double>) mr.movieToProducers.get(mid);
                        }
                                   
                        return new HashMap<String,Double>(); 	
                    }
                    
                        
/***********************************************************************************************************************/
    
    /**
     * Returns all Genres against a movie
     * 
     * It is returned as an array of genres.
     * @param mid the movie id.
     * @return an array of genres.
     */
    
    public int getGenreSize()     
    {
        return mr.movieToGenre.size();         
         	
    }
    
/***********************************************************************************************************************/
    
    /**
     * Returns all movies/ratings that a particular user has rated.
     * 
     * It is returned as an array of mid/rating blocks.
     * @param uid the user id
     * @return an array of mid/rating blocks
     */
    public LongArrayList getMoviesSeenByUser(int uid) 
    {
        if (mr.custToMovie.containsKey(uid))        
        {
            return (LongArrayList) mr.custToMovie.get(uid);
        }
        return new LongArrayList();
    }

/***********************************************************************************************************************/
/***********************************************************************************************************************/

    /**
     * Inner joins together the data from two uids or two mids, depending.
     * 
     * To explain: if you want to find all the movies in common
     * between two users, you'd put true into onMovies, then enter
     * in two different user ids into the other parameters.  
     * you now have 
     * 
     * @param a movie id or user id one
     * @param b movie id or user id two
     * @param which true if the parameters are user ids, false if the
     * parameters are movie ids
     * @return a join between the two users/movies, as uid/rating or mid/rating
     * blocks (depending)
     */
    
    public ArrayList<Pair> innerJoinOnMoviesOrRating(int a, int b, boolean which)     
    {
        // Get the movies/users for each parameter
        LongArrayList left, right;
        
        //Do they returns the sorted (in ascending order?)????
        
        if(which) //parameters passed are user ids        
        {
            left = getMoviesSeenByUser(a);
            right = getMoviesSeenByUser(b);		//list of movies
        }
        
        else //As is case with item based CF, (similarities between items)         
        {
            left = getUsersWhoSawMovie(a);
            right = getUsersWhoSawMovie(b);		//list of users
        }

        // Join the two using a sort-merge join
        // Assumes that they two lists are already sorted
        
        ArrayList<Pair> match = new ArrayList<Pair>();  //It has defualt size of zero
        
        int leftIndex = 0, rightIndex = 0;       
        
        while (leftIndex < left.size() && rightIndex < right.size())        
        {
        	//let us suppose , we got list of movies (i.e. left list and right list of movies)
        	
           	if (parseUserOrMovie(left.getQuick(leftIndex))			//if leftmovielist.get(0++) == rightmovielist.get(0++)-->match		 
                == parseUserOrMovie(right.getQuick(rightIndex)))        	
        	{
                match.add(new Pair(left.getQuick(leftIndex++),
                                   right.getQuick(rightIndex++)));   //for movies: add list of uid/ratings
            }														 //for users: add list of mid/ratings	
            
        	else if (parseUserOrMovie(left.getQuick(leftIndex)) 
                    < parseUserOrMovie(right.getQuick(rightIndex)))        	
        	{
                leftIndex++;
            }
            
        	else        	
        	{
                rightIndex++;
            }
        }

        return match;
    }
   
    
/***********************************************************************************************************************/    
/**
 * Same: but do one thing, apply only those user/items, found as neighbour
 */
    public ArrayList<Pair> innerJoinOnMoviesOrRating(int a, int b, boolean which, IntArrayList myUsersMoviesFiletr)     
    {
        // Get the movies/users for each parameter
        LongArrayList left, right;
        
        //Do they returns the sorted (in ascending order?)????
        
        if(which) //parameters passed are user ids        
        {
            left = getMoviesSeenByUser(a);
            right = getMoviesSeenByUser(b);		//list of movies
        }
        
        else //As is case with item based CF, (similarities between items)         
        {
            left = getUsersWhoSawMovie(a);
            right = getUsersWhoSawMovie(b);		//list of users
        }

        
     
        // Join the two using a sort-merge join
        // Assumes that they two lists are already sorted
        
        ArrayList<Pair> match = new ArrayList<Pair>();  //It has defualt size of zero
        
        int leftIndex = 0, rightIndex = 0;       
        
        while (leftIndex < left.size() && rightIndex < right.size())        
        {
        	//let us suppose , we got list of movies (i.e. left list and right list of movies)
        	
           	if (parseUserOrMovie(left.getQuick(leftIndex))			//if leftmovielist.get(0++) == rightmovielist.get(0++)-->match		 
                == parseUserOrMovie(right.getQuick(rightIndex))
                && myUsersMoviesFiletr.contains(parseUserOrMovie(left.getQuick(leftIndex)))
                && myUsersMoviesFiletr.contains(parseUserOrMovie(right.getQuick(rightIndex)))) //Filter		
        	{
             
           		match.add(new Pair(left.getQuick(leftIndex++),
                                   right.getQuick(rightIndex++)));   //for movies: add list of uid/ratings
            }														 //for users: add list of mid/ratings	
            
        	else if (parseUserOrMovie(left.getQuick(leftIndex)) 
                    < parseUserOrMovie(right.getQuick(rightIndex)))        	
        	{
                leftIndex++;
            }
            
        	else        	
        	{
                rightIndex++;
            }
        }

        return match;
    }
   
 /***********************************************************************************************************************/
    
    /**
     * Full outer joins together the data from two uids or two mids, depending.
     * 
     * To explain: if you want to find all the movies in common
     * between two users, you'd put true into onMovies, then enter
     * in two different user ids into the other parameters.  Bam, 
     * you now have 
     * 
     * @param a movie id or user id one
     * @param b movie id or user id two
     * @param which true if the parameters are user ids, false if the
     * parameters are movie ids
     * @return a join between the two users/movies, as uid/rating or mid/rating
     * blocks (depending)
     */
    
    public ArrayList<Pair> fullOuterJoinOnMoviesOrRating(int a, int b, boolean which)     
    {
        // Get the movies/users for each parameter
        LongArrayList left, right;
    
        if(which)        
        {
            left = getMoviesSeenByUser(a);
            right = getMoviesSeenByUser(b);
        }
        
        else
        {
            left = getUsersWhoSawMovie(a);
            right = getUsersWhoSawMovie(b);
        }

        // Join the two using a sort-merge join
        // Assumes that they two lists are already sorted
        ArrayList<Pair> match = new ArrayList<Pair>();
        
        int leftIndex = 0, rightIndex = 0;
        
        while (leftIndex < left.size() && rightIndex < right.size()) 
        
        {
            if (parseUserOrMovie(left.getQuick(leftIndex)) 
                == parseUserOrMovie(right.getQuick(rightIndex))) 
            
            {
                match.add(new Pair(left.getQuick(leftIndex++),				//adding new pair in match
                                   right.getQuick(rightIndex++)));
            }
            
            else if(parseUserOrMovie(left.getQuick(leftIndex))
                    < parseUserOrMovie(right.getQuick(rightIndex))) 
            
            {
                match.add(new Pair(left.getQuick(leftIndex++), 0));
            }
           
            else 
            
            {
                match.add(new Pair(0, right.getQuick(rightIndex++)));
            }
        }

        return match;
    }

 /***********************************************************************************************************************/
    
    //??????????????????????
    
    /**
     * Reads a serialzed OpenIntObjectHashMap containing
     * a mapping from mid to movie name. 
     *
     * @param  filename  The serialized OpenIntObjectHashMap
     */
    
    public void readNames(String filename)     
    {

        try
        {

            FileInputStream fis  = new FileInputStream(filename);
            ObjectInputStream in = new ObjectInputStream(fis);
            
            midToName = (OpenIntObjectHashMap) in.readObject(); 		//it should be an object which maps mid to mName
        }
        
        catch(ClassNotFoundException e) 
        
        {
            System.out.println("Can't find class");
            e.printStackTrace();
        }
        
        catch(IOException e) {
        
        	System.out.println("IO error");
            e.printStackTrace();
        }
    }
    
/***********************************************************************************************************************/
    /**
     * Returns the movie name for a given mid. 
     *
     * @param  mid  The movie id to look up. 
     */
    
    public String getMovieName(int mid)     
    {

        if(midToName == null)         
        {
            throw new RuntimeException("movie names not loaded");
        }
                
        if(!midToName.containsKey(mid))        
        {
            return "Error, Movie not in DB.";
        }
        
        else        
        {
            return (String) midToName.get(mid);			//?
        }
    }
    
 /***********************************************************************************************************************/
    
	
	/**
	 * @author steinbel, based off getCommonUserRatAndAve by tuladara
	 * Gets a list of ratings and averages for movies seen in common by two users.
	 * @param uid1 - id of one of the users
	 * @param uid2 - id of the other user
	 * @return - list where each entry is
	 *  <user 1's rating on movie x, user2's rating on movie x, average rating for movie x>
	 */
	public ArrayList<Triple> getCommonMovieRatAndAve(int uid1, int uid2)	
	{
		ArrayList<Pair> justCommonRatings = innerJoinOnMoviesOrRating(uid1, uid2, true);
		ArrayList<Triple> commonMovieAverages = new ArrayList<Triple>();
	
		for (Pair justCommon : justCommonRatings) 		
		{
			commonMovieAverages.add(new Triple(
					MemHelper.parseRating(justCommon.a),
					MemHelper.parseRating(justCommon.b),
					getAverageRatingForMovie(MemHelper.parseUserOrMovie(justCommon.a))));
		}
		
		return commonMovieAverages;
	}

/***********************************************************************************************************************/

	//Each justCommonUserAndAve contains a user (userID+ratingforMovie1, userID+ratingforMovie2)
    public ArrayList<Triple> getCommonUserRatAndAve(int mId1, int mId2)    
    {
        ArrayList<Pair> justCommonRatings = innerJoinOnMoviesOrRating(mId1, mId2, false);
        ArrayList<Triple> commonUserAverages = new ArrayList<Triple>();
    
        for(Pair justCommonRating : justCommonRatings)         
        {
            commonUserAverages.add(new Triple(
                    MemHelper.parseRating(justCommonRating.a), 
                    MemHelper.parseRating(justCommonRating.b),
                    getAverageRatingForUser(MemHelper.parseUserOrMovie(justCommonRating.a))));
        }
        
        return commonUserAverages;
    }
    
/******************************************************************************************************/    

 /**
 *  same, but find only those users, found in the user-based CF as neighbours
 */
 
    public ArrayList<Triple> getCommonUserRatAndAve(int mId1, int mId2, IntArrayList myNeighbours)    
    {
        ArrayList<Pair> justCommonRatings = innerJoinOnMoviesOrRating(mId1, mId2, false, myNeighbours);
        ArrayList<Triple> commonUserAverages = new ArrayList<Triple>();
    
        for(Pair justCommonRating : justCommonRatings)         
        {
            commonUserAverages.add(new Triple(
                    MemHelper.parseRating(justCommonRating.a), 
                    MemHelper.parseRating(justCommonRating.b),
                    getAverageRatingForUser(MemHelper.parseUserOrMovie(justCommonRating.a))));
        }
        
        return commonUserAverages;
    }
    
/******************************************************************************************************/
    /**
     *  get all the userIds and with ratings who rated a movie
     *  @param mid
     *  @return ArrayList of pairs  <UserID, MoviesRating> 
     */

    public ArrayList<Pair> getRatingVector(int movieID)    
    {
    	
    	ArrayList<Pair> vector = new ArrayList<Pair>();
    	
    	LongArrayList myUsers = getUsersWhoSawMovie (movieID);
	
    	for(int i=0;i<myUsers.size();i++)
    	{
    		int uid = MemHelper.parseUserOrMovie(myUsers.getQuick(i));    	    
    		double rating = getRating(uid, movieID);
    		double uAvg = getAverageRatingForUser(uid);
    		vector.add(new Pair(uAvg, rating));			//user ID and rating for a movie
    	}
    	
		return vector;
    }
   

/***********************************************************************************************************************/
}

