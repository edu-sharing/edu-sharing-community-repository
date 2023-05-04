import { Pipe, PipeTransform } from '@angular/core';
import { ConfigService, Person } from 'ngx-edu-sharing-api';

@Pipe({ name: 'nodePersonName' })
export class NodePersonNamePipe implements PipeTransform {
    constructor(private config: ConfigService) {}
    transform(person: Person | any, args: any = null): string {
        let field = this.config.instant('userDisplayName', 'fullName');
        if (person == null) return null;
        if (field == 'authorityName') {
            if (person.authorityName == null) field = 'fullName';
            else return person.authorityName;
        }
        if (field == 'fullName') {
            if (person.profile) {
                return (
                    (person.profile.firstName ? person.profile.firstName : '') +
                    ' ' +
                    (person.profile.lastName ? person.profile.lastName : '')
                ).trim();
            }
            return (
                (person.firstName ? person.firstName : '') +
                ' ' +
                (person.lastName ? person.lastName : '')
            ).trim();
        }
        if (field == 'firstName' || field == 'lastName') {
            if (person.profile) {
                return person.profile[field];
            }
            return person[field];
        }
        if (field == 'email') {
            if (person.profile && person.profile.email) return person.profile.email;
            if (person.email == null) return person.mailbox;
            return person.email;
        }
        return person[field];
    }
}
