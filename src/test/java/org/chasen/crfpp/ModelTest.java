package org.chasen.crfpp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ModelTest {
  
  @Test
  public void simpleTest() {
    
    Model model = new Model("-m ./src/test/resources/test-model");
    
    // create Tagger from Model
    Tagger tagger = model.createTagger();
    
    // clear internal context
    tagger.clear();
    
    // add context
    tagger.add("Confidence NN");
    tagger.add("in IN");
    tagger.add("the DT");
    tagger.add("pound NN");
    tagger.add("is VBZ");
    tagger.add("widely RB");
    tagger.add("expected VBN");
    tagger.add("to TO");
    tagger.add("take VB");
    tagger.add("another DT");
    tagger.add("sharp JJ");
    tagger.add("dive NN");
    tagger.add("if IN");
    tagger.add("trade NN");
    tagger.add("figures NNS");
    tagger.add("for IN");
    tagger.add("September NNP");
    
    assertEquals((int) tagger.size(), 17);
    assertEquals((int) tagger.xsize(), 1);
    assertEquals((int) tagger.ysize(), 2);
    
    assertTrue(tagger.parse());
  }
  
}
