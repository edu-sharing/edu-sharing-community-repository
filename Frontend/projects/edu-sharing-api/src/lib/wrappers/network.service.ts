import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { NetworkV1Service } from '../api/services';
import { shareReplayReturnValue } from '../utils/decorators';

interface Repository {
    id: string;
    title: string;
    icon: string;
    logo: string;
    isHomeRepo: boolean;
    repositoryType: string;
    renderingSupported: boolean;
}

interface NetworkRepositories {
    repositories: Repository[];
}

@Injectable({
    providedIn: 'root',
})
export class NetworkService {
    constructor(private networkV1: NetworkV1Service) {}

    @shareReplayReturnValue()
    getRepositories(): Observable<Repository[]> {
        return this.networkV1
            .getRepositories()
            .pipe(map((repos) => (repos as unknown as NetworkRepositories).repositories));
    }
}
