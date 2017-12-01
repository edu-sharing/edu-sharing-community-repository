import { AngularEdusharingPage } from './app.po';

describe('angular-edusharing App', function() {
  let page: AngularEdusharingPage;

  beforeEach(() => {
    page = new AngularEdusharingPage();
  });

  it('should display message saying app works', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('app works!');
  });
});
