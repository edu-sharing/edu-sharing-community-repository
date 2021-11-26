import {Component} from '@angular/core';
import {Collection, RestCollectionService, RestConnectorService, RestConstants} from '../../../core-module/core.module';

@Component({
  selector: 'es-user-avatar-test',
  styleUrls: ['./user-avatar-test.component.scss'],
  templateUrl: './user-avatar-test.component.html',
})
export class UserAvatarTestComponent {
  sizes = ['xsmall', 'small', 'medium', 'large'];
  users = [
      {authorityName: 'Bernd'},
      {authorityName: 'GROUP_SCHOOL', authorityType: 'GROUP', profile: {displayName: 'School Group XYZ'}},
      {authorityName: 'GROUP_EDITORIAL', authorityType: 'GROUP', profile: {displayName: 'Editorial Group XYZ', groupType: RestConstants.GROUP_TYPE_EDITORIAL}},
      {authorityName: 'Max'},
      {authorityName: 'Jon'},
      {authorityName: 'Jack Editorial', profile: {firstName: 'Jack', lastName: 'Editorial', types: [RestConstants.GROUP_TYPE_EDITORIAL]} },
      {authorityName: 'Bar Editorial', profile: {firstName: 'Bar', lastName: 'Editorial', types: [RestConstants.GROUP_TYPE_EDITORIAL]} },
      ];
}

