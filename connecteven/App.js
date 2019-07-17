/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow
 */

import React, {Component} from 'react';
import {Platform, StyleSheet, Text, View,DeviceEventEmitter} from 'react-native';
import FFTRecoder from "./nativeModule";
import Permissions from 'react-native-permissions'

const instructions = Platform.select({
  ios: 'Press Cmd+R to reload,\n' + 'Cmd+D or shake for dev menu',
  android:
    'Double tap R on your keyboard to reload,\n' +
    'Shake or press menu bufdfdftton for dev menu',
});

FFTRecoder.show('Awesome', FFTRecoder.SHORT);

export default class App extends Component {

  componentDidMount() {
    this.subscription = DeviceEventEmitter.addListener('qui', (e)=> {
      console.log("even file");
      console.log(e);
      // handle event
    });

    Permissions.request('microphone', { type: 'always' }).then(response => {
      FFTRecoder.startRecording();
    })
  }

  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>Welcome to React Native!</Text>
        <Text style={styles.instructions}>To get started, edit App.js</Text>
        <Text style={styles.instructions}>{instructions}</Text>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});
