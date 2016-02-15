# PunORama

## Definition

PunORama is a pun finder. I define a pun as follows: two words, where the end of the first overlaps with the beginning of the second. 

I'm finding puns from words in the English dictionary which comes with Linux: `/usr/share/dict/american-english`, which I filtered: `grep "^[a-zA-Z]*$" /usr/share/dict/words > words_no_punctuation`.

The puns are scored by how much overlap there is between the two words. I tried to eliminate lame puns, such as puns that are composed by two words that are almost identical (e.g., cat & cats).

The [app](https://punoramainsight.appspot.com) offers 4 functions. 

  - The button `Make a pun!` allows the user to finds a pun based on her input (the input will be either the first or the second word). 
  
  - The button `Make a tree!` builds a tree (using D3), which shows puns for the 10 semantically closest words to the one the user entered (the input word or related word will always be the first word of the puns).
  
  - The button `More puns!` shows a list of the best puns (sorted by score).
  
  - The tab `Trending Tweets` shows a list of trending tweets based on a file. This only works when the file is being read. This process needs to be manually started. 

## Implementation

This project was entirely developped using Google Cloud Platform:

  - The dictionary and the tweet file, as well as all intermediate files are stored in Google Storage. 

  - The algorithm to find the puns is implemented in Google Dataflow. I'm using a Pub/Sub topic to simulate the stream of tweets from the aformentioned file. 

  - The puns and the 10 semantically closest words to all the words in the dictionary are stored in tables in Google BigQuery.
  
  - The app was deployed using the Goole App Engine.

In order to get words that are semantically closest to a user's input, I used a [word2vec](https://code.google.com/archive/p/word2vec/) pre-trained word vector. word2vec is a deep learning model that contains 300-dimensional vectors for 3 million words and phrases. I ran the `distance` binary on the dictionary I'm using: `./distance GoogleNews-vectors-negative300.bin > related_words.txt < words_no_punctuation`.

Puns are found when there is an overlap between the pronunciation of the end of a word and the beginning of another. To get the pronunciation of words, I used an open source sofware called [espeak](http://espeak.sourceforge.net/). 

## Performance

I implemented 2 algorithms to find puns:

- A cartesian product algorithm, creates a list of all possible pairs of words.

- A trie algorithm, builds a trie with the dictionary. 

The cartesian product runs in about 18 hours on 3 [n1-standard-1](https://cloud.google.com/compute/pricing#machinetype). The trie algorithm takes about 1 hour on the same cluster. 

## TODO

Add a guide to the code.

My code is in `src/main/scala and src/test/scala`; the Java code is Google's.