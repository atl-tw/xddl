/*
 * Copyright 2019 Robert Cooper, ThoughtWorks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.kebernet.xddl.editor.core;

import java.awt.Component;
import javax.swing.JLabel;

/** @author rcooper */
public class DisclosureBehavior {
  private final JLabel twist;
  private final Component body;
  private boolean expanded = false;

  public DisclosureBehavior(JLabel twist, Component body) {
    this.twist = twist;
    this.body = body;

    apply();
  }

  public void setExpanded(boolean expanded) {
    this.expanded = expanded;
    apply();
  }

  public void apply() {
    body.setVisible(expanded);
    this.twist.setText(expanded ? "\u25BC" : "\u25B6");
  }

  void toggleExpanded() {
    this.setExpanded(!expanded);
  }
}
