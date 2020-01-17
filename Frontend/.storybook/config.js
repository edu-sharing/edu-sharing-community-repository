import { configure } from '@storybook/angular';
import '!style-loader!css-loader!sass-loader!./scss-loader.scss';

function loadStories() {
    require('../stories/index.js');
    // You can require as many stories as you need.
}

configure(loadStories, module);