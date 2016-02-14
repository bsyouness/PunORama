# PunORama
## Project Definition
PunORama is a pun finder. I define a pun as follows: two words, where the end of the first overlaps with the beginning of the second. 
I'm finding puns from words in the English dictionary which comes with Linux: `/usr/share/dict/american-english`, which I filtered: `grep "^[a-zA-Z]*$" /usr/share/dict/words > words_no_punctuation`.

The puns are scored by how much overlap there is between the two words. I tried to eliminate lame puns, such as puns that are composed by two words that are almost identical (e.g., cat & cats).

The [app](punoramainsight.appspot.com) offers 4 functions. 
  - The button `Make a pun!` allows the user to finds a pun based on her input (the input will be either the first or the second word). 
  - The button `Make a tree!` builds a tree (using D3), which shows puns for the 10 semantically closest words (see note below) to the one the user entered (the input word or related word will always be the first word of the puns).
  - The button `More puns!` shows a list of the best puns (sorted by score).
  - The tab `Trending Tweets` shows a list of trending tweets based on a file saved on Google Storage. This only works when the file is being read. This process needs to be manually started. 

Note: In order to get words that are semantically closest to a user's input, I used a [word2vec](https://code.google.com/archive/p/word2vec/) pre-trained word vector. Google published pre-trained vectors trained on part of Google News dataset (about 100 billion words). The model contains 300-dimensional vectors for 3 million words and phrases. I ran the `distance` binary on the whole dictionary: `./distance GoogleNews-vectors-negative300.bin > related_words.txt < words_no_punctuation`.

## TODO
Add a guide to the code.
My code is in src/main/scala and src/test/scala; the Java code is Google's.