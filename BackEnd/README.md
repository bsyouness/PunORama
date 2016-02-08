# PunORama

I'm using the Linux native English dictionary (/usr/share/dict/american-english), to find puns.

In order to obtain words that are similar to a user's input, I used a [word2vec](https://code.google.com/archive/p/word2vec/) pre-trained word vector. Google published pre-trained vectors trained on part of Google News dataset (about 100 billion words). The model contains 300-dimensional vectors for 3 million words and phrases.

I ran the `distance` binary on my entire dictionary to get the 10 semantically closest words to each word of the dictionary.

`/output/file > distance < cat /usr/share/dict/american-english`

`grep "^[a-zA-Z]*$" /usr/share/dict/words > words_no_punctuation`

`./distance GoogleNews-vectors-negative300.bin > /usr/local/google/home/ericmc/t/2016_q1/word2vec_no_punctuation.txt < words_no_punctuation`